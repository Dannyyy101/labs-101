#!/usr/bin/env python3
"""
Streamt den Open Food Facts CSV-Export (tab-separiert, ~6 GB) per COPY direkt
in die Postgres-Tabelle open_food. Kein Umweg über HTTP/Hibernate.

Ablauf:
  1. UNLOGGED Staging-Tabelle anlegen (keine Indizes, kein WAL -> schnell)
  2. Bereinigte Zeilen als COPY-Stream reinschreiben
  3. Per INSERT ... ON CONFLICT nach open_food mergen (dedupliziert Barcodes)
  4. Staging droppen

Beispiel:
    python off_copy_import.py \
        --dsn "postgresql://user:pass@localhost:5432/labs101" \
        --file en.openfoodfacts.org.products.csv

Benötigt: pip install psycopg2-binary
"""

import argparse
import csv
import io
import itertools
import os
import sys
import time

import psycopg2

# --------------------------------------------------------------------------
# Spalten-Mapping: Zielspalte -> Spaltenname im OFF-Export
# --------------------------------------------------------------------------
COLUMNS = {
    "bar_code": "code",
    "name": "product_name",
    "company": "brands",
    "kcal": "energy-kcal_100g",
    "water": "water_100g",
    "protein": "proteins_100g",
    "fat": "fat_100g",
    "carbohydrates": "carbohydrates_100g",
    "fiber": "fiber_100g",
}

# Reihenfolge der Spalten im COPY-Stream
TARGET_COLUMNS = [
    "bar_code", "name", "company",
    "kcal", "water", "protein", "fat", "carbohydrates", "fiber",
]

NUMERIC_COLUMNS = {"kcal", "water", "protein", "fat", "carbohydrates", "fiber"}

# Plausibilitätsgrenzen pro 100 g. OFF ist Crowdsourcing und enthält
# jede Menge Unsinn (Fett = 12000 g/100 g o.ä.).
LIMITS = {
    "kcal": 900.0,
    "water": 100.0,
    "protein": 100.0,
    "fat": 100.0,
    "carbohydrates": 100.0,
    "fiber": 100.0,
}

MAX_NAME_LEN = 255
MAX_COMPANY_LEN = 255

STAGING_TABLE = "open_food_staging"


# --------------------------------------------------------------------------
# Parsing
# --------------------------------------------------------------------------
def to_double(raw, field):
    if raw is None:
        return None
    raw = raw.strip()
    if not raw:
        return None
    try:
        value = float(raw)
    except ValueError:
        return None
    if value != value or value in (float("inf"), float("-inf")):
        return None
    if value < 0 or value > LIMITS.get(field, 1e9):
        return None
    return round(value, 3)


def clean_text(raw, max_len):
    if not raw:
        return None
    text = " ".join(raw.split())
    if not text:
        return None
    return text[:max_len]


def escape(value):
    """Escaping für das COPY-Textformat. None wird zu \\N (SQL NULL)."""
    if value is None:
        return "\\N"
    return (
        value.replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    )


def build_line(values, idx):
    """Eine CSV-Zeile -> fertige COPY-Zeile (str), oder None wenn unbrauchbar."""
    def col(name):
        i = idx.get(name)
        if i is None or i >= len(values):
            return None
        return values[i]

    bar_code = clean_text(col(COLUMNS["bar_code"]), 64)
    name = clean_text(col(COLUMNS["name"]), MAX_NAME_LEN)
    if not bar_code or not name:
        return None

    kcal = to_double(col("energy-kcal_100g"), "kcal")
    if kcal is None:
        kj = to_double(col("energy_100g"), "kcal")
        if kj is None:
            kj = to_double(col("energy-kj_100g"), "kcal")
        if kj is not None:
            kcal = round(kj / 4.184, 3)

    row = {
        "bar_code": bar_code,
        "name": name,
        "company": clean_text(col(COLUMNS["company"]), MAX_COMPANY_LEN),
        "kcal": kcal,
        "water": to_double(col(COLUMNS["water"]), "water"),
        "protein": to_double(col(COLUMNS["protein"]), "protein"),
        "fat": to_double(col(COLUMNS["fat"]), "fat"),
        "carbohydrates": to_double(col(COLUMNS["carbohydrates"]), "carbohydrates"),
        "fiber": to_double(col(COLUMNS["fiber"]), "fiber"),
    }

    fields = []
    for column in TARGET_COLUMNS:
        value = row[column]
        if column in NUMERIC_COLUMNS:
            fields.append("\\N" if value is None else repr(value))
        else:
            fields.append(escape(value))
    return "\t".join(fields) + "\n"


def iter_lines(path, limit=0, progress_every=250_000):
    """Generator über fertige COPY-Zeilen. Hält immer nur eine Zeile im RAM."""
    csv.field_size_limit(sys.maxsize)
    started = time.time()
    produced = 0
    skipped = 0

    # errors="replace": im Export stecken einzelne kaputte UTF-8-Sequenzen.
    with open(path, "r", encoding="utf-8", errors="replace", newline="") as fh:
        # QUOTE_NONE ist Pflicht: OFF escaped Anführungszeichen nicht.
        reader = csv.reader(fh, delimiter="\t", quoting=csv.QUOTE_NONE)
        try:
            header = next(reader)
        except StopIteration:
            return
        idx = {col.strip(): i for i, col in enumerate(header)}

        missing = [c for c in COLUMNS.values() if c not in idx]
        if missing:
            print(f"WARNUNG: Spalten fehlen im Export: {missing}", file=sys.stderr)
        if COLUMNS["bar_code"] not in idx or COLUMNS["name"] not in idx:
            raise SystemExit("Abbruch: 'code' oder 'product_name' fehlt.")

        for values in reader:
            try:
                line = build_line(values, idx)
            except Exception as exc:
                skipped += 1
                print(f"Zeile übersprungen: {exc}", file=sys.stderr)
                continue
            if line is None:
                skipped += 1
                continue

            yield line
            produced += 1

            if produced % progress_every == 0:
                elapsed = time.time() - started
                print(f"{produced:>10,} Zeilen gestreamt "
                      f"({produced / elapsed:,.0f}/s, {skipped:,} verworfen)")
            if limit and produced >= limit:
                break

    elapsed = time.time() - started
    print(f"Stream fertig: {produced:,} Zeilen in {elapsed:.0f}s, "
          f"{skipped:,} verworfen")


class LineStream(io.RawIOBase):
    """Macht aus dem Zeilen-Generator ein read()-fähiges Objekt für copy_expert."""

    def __init__(self, lines):
        self._lines = lines
        self._buffer = b""

    def readable(self):
        return True

    def readinto(self, target):
        wanted = len(target)
        while len(self._buffer) < wanted:
            try:
                self._buffer += next(self._lines).encode("utf-8")
            except StopIteration:
                break
        if not self._buffer:
            return 0
        chunk = self._buffer[:wanted]
        self._buffer = self._buffer[len(chunk):]
        target[:len(chunk)] = chunk
        return len(chunk)


# --------------------------------------------------------------------------
# SQL
# --------------------------------------------------------------------------
CREATE_STAGING = f"""
DROP TABLE IF EXISTS {STAGING_TABLE};
CREATE UNLOGGED TABLE {STAGING_TABLE} (
    bar_code        text,
    name            text,
    company         text,
    kcal            double precision,
    water           double precision,
    protein         double precision,
    fat             double precision,
    carbohydrates   double precision,
    fiber           double precision
);
"""

ENSURE_UNIQUE = """
CREATE UNIQUE INDEX IF NOT EXISTS uk_open_food_bar_code
    ON open_food (bar_code);
"""

MERGE = f"""
INSERT INTO open_food
    (bar_code, name, company, kcal, water, protein, fat, carbohydrates, fiber,
     create_date, update_date)
SELECT DISTINCT ON (bar_code)
    bar_code, name, company, kcal, water, protein, fat, carbohydrates, fiber,
    now(), now()
FROM {STAGING_TABLE}
ORDER BY bar_code, (kcal IS NULL), (protein IS NULL), length(name) DESC
ON CONFLICT (bar_code) DO UPDATE SET
    name          = EXCLUDED.name,
    company       = EXCLUDED.company,
    kcal          = EXCLUDED.kcal,
    water         = EXCLUDED.water,
    protein       = EXCLUDED.protein,
    fat           = EXCLUDED.fat,
    carbohydrates = EXCLUDED.carbohydrates,
    fiber         = EXCLUDED.fiber,
    update_date   = now();
"""


def main():
    parser = argparse.ArgumentParser(
        description="Open Food Facts CSV -> Postgres (COPY)")
    parser.add_argument("--file", required=True, help="Pfad zum OFF-CSV")
    parser.add_argument("--dsn", default=os.environ.get("DATABASE_URL"),
                        help="postgresql://user:pass@host:5432/db "
                             "(oder ENV DATABASE_URL)")
    parser.add_argument("--limit", type=int, default=0,
                        help="nur N gültige Produkte (0 = alle)")
    parser.add_argument("--truncate", action="store_true",
                        help="open_food vorher leeren")
    parser.add_argument("--keep-staging", action="store_true",
                        help="Staging-Tabelle nicht droppen (zum Nachschauen)")
    parser.add_argument("--dry-run", action="store_true",
                        help="nur die ersten 20 COPY-Zeilen ausgeben")
    args = parser.parse_args()

    if args.dry_run:
        for line in itertools.islice(iter_lines(args.file, limit=20), 20):
            sys.stdout.write(line)
        return

    if not args.dsn:
        raise SystemExit("Kein --dsn und kein DATABASE_URL gesetzt.")

    started = time.time()
    conn = psycopg2.connect(args.dsn)
    try:
        with conn.cursor() as cur:
            print("Lege Staging-Tabelle an…")
            cur.execute(CREATE_STAGING)
            conn.commit()

            print("COPY läuft…")
            copy_started = time.time()
            stream = io.BufferedReader(LineStream(iter_lines(args.file, args.limit)),
                                       buffer_size=1 << 20)
            cur.copy_expert(
                f"COPY {STAGING_TABLE} ({', '.join(TARGET_COLUMNS)}) "
                f"FROM STDIN WITH (FORMAT text)",
                stream,
            )
            staged = cur.rowcount
            conn.commit()
            print(f"COPY fertig: {staged:,} Zeilen in "
                  f"{time.time() - copy_started:.0f}s")

            cur.execute(f"ANALYZE {STAGING_TABLE}")

            if args.truncate:
                print("Leere open_food…")
                cur.execute("TRUNCATE open_food RESTART IDENTITY")

            print("Stelle Unique-Index auf bar_code sicher…")
            cur.execute(ENSURE_UNIQUE)
            conn.commit()

            print("Merge nach open_food…")
            merge_started = time.time()
            cur.execute(MERGE)
            merged = cur.rowcount
            conn.commit()
            print(f"Merge fertig: {merged:,} Zeilen in "
                  f"{time.time() - merge_started:.0f}s")

            if not args.keep_staging:
                cur.execute(f"DROP TABLE {STAGING_TABLE}")
                conn.commit()

            cur.execute("ANALYZE open_food")
            conn.commit()

            cur.execute("SELECT count(*) FROM open_food")
            total = cur.fetchone()[0]
    finally:
        conn.close()

    print(f"\nFertig in {(time.time() - started) / 60:.1f} min. "
          f"open_food enthält jetzt {total:,} Produkte.")


if __name__ == "__main__":
    main()
