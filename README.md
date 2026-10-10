# labs-101

Persönliche Health- & Fitness-Plattform: Ernährung tracken, Workouts planen und loggen, Läufe auswerten und Apple-Health-Daten zentral speichern.

Das Projekt besteht aus drei Clients/Diensten, die sich eine Postgres-Datenbank teilen:

| Teil | Technik | Aufgabe |
| --- | --- | --- |
| [`backend/`](backend/) | Spring Boot 4 · Java 25 · JPA · Flyway | REST-API unter `/api`, Geschäftslogik, Datenbank-Migrationen |
| [`frontend/`](frontend/) | Next.js 16 · React 19 · Tailwind 4 · shadcn · Better Auth | Web-App mit Login, Dashboards und Formularen |
| [`ios/`](ios/) | SwiftUI · HealthKit (iOS 26.5) | Apple-Health-Sync, Essen- und Workout-Tracking unterwegs |
| Postgres | Docker | gemeinsame Datenbank `labs101` (App-Daten + Auth-Tabellen) |

```mermaid
flowchart LR
    iOS["iOS-App<br/>(HealthKit)"] -- "HTTP + X-API-Key" --> API
    Web["Next.js-Frontend<br/>:3000"] -- "HTTP + X-API-Key" --> API["Spring-Backend<br/>:8080/api"]
    Web -- "Better Auth" --> DB
    API --> DB[("Postgres<br/>labs101")]
```

## Features

- **Ernährung** – Lebensmittel suchen (Trigram-Volltextsuche), per Barcode scannen, aus Freitext extrahieren und mit Portionen tracken. Datenbasis: Bundeslebensmittelschlüssel (BLS) und Open Food Facts.
- **Workouts** – Übungen mit trainierten Muskeln, Workout-Vorlagen, Live-Sessions, Statistiken und Muskelkarte.
- **Läufe** – Auswertung der aus Apple Health synchronisierten Läufe inkl. GPS-Route auf der Karte.
- **Apple-Health-Sync** – die iOS-App spiegelt HealthKit-Samples ins Backend; das Backend kann umgekehrt Schreibaufträge hinterlegen. Details: [`documents/health-sync.md`](documents/health-sync.md).
- **Health-Übersicht & Planer** – Startseite mit Kennzahlen, Kalender für geplante Einheiten.
- **Einstellungen** – nutzerbezogene Settings.

## Schnellstart mit Docker

Voraussetzung: Docker mit Compose.

```sh
cp .env.example .env   # und Werte ergänzen, siehe unten
docker compose up -d --build
```

Danach laufen:

- Frontend: <http://localhost:3000>
- Backend: <http://localhost:8080/api>
- Postgres: `localhost:5432`, Datenbank `labs101`

Die Datenbank-Migrationen (Flyway, [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration)) laufen beim Start des Backends automatisch.

### Umgebungsvariablen

Die `.env` im Repo-Root wird von Docker Compose und der VS-Code-Launch-Config des Backends gelesen.

| Variable | Genutzt von | Beschreibung |
| --- | --- | --- |
| `POSTGRES_USERNAME` / `POSTGRES_PASSWORD` | DB, Backend, Frontend | Zugangsdaten der Datenbank |
| `POSTGRES_HOST` | Frontend | Host der DB für Better Auth (in Docker: `db`) |
| `API_KEY` | Backend, Frontend, iOS | Gemeinsamer Schlüssel im Header `X-API-Key`. **Ohne gesetzten Key lehnt das Backend alle Requests ab.** |
| `BETTER_AUTH_URL` | Frontend | Öffentliche URL des Frontends |
| `BETTER_AUTH_SECRET` | Frontend | Secret für Sessions |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Frontend | Google-Login |
| `ALLOWED_EMAIL_ADDRESSES` | Frontend | Kommagetrennte Liste der E-Mail-Adressen, die sich registrieren/anmelden dürfen |

## Lokale Entwicklung

Für die Entwicklung reicht es, nur die Datenbank in Docker zu starten:

```sh
docker compose up -d db
```

### Backend

Benötigt JDK 25.

```sh
cd backend
API_KEY=dev-key ./mvnw spring-boot:run
```

Die Standard-Verbindung (`localhost:5432`, `user` / `abc123`) steht in [`application.properties`](backend/src/main/resources/application.properties) und passt zu `.env.example`. Alternativ in VS Code die Launch-Config „Spring Boot-BackendApplication“ starten – sie lädt die `.env` automatisch.

Tests (u. a. mit Embedded Postgres):

```sh
cd backend
./mvnw verify
```

### Frontend

Benötigt Node.js 22+. Die Variablen aus der Tabelle oben gehören in `frontend/.env`, zusätzlich `BACKEND_URL=http://localhost:8080/api`.

```sh
cd frontend
npm install
npm run dev     # http://localhost:3000
npm run lint
npm run build
```

Die Tabellen für Better Auth liegen als SQL in [`frontend/better-auth_migrations/`](frontend/better-auth_migrations/) und müssen einmalig in die Datenbank eingespielt werden.

### iOS-App

Benötigt Xcode 26 und ein Gerät bzw. einen Simulator mit iOS 26.5.

1. Secrets anlegen:
   ```sh
   cp ios/Config/Secrets.example.xcconfig ios/Config/Secrets.xcconfig
   ```
   und `API_BASE_URL`, `API_KEY` und `USER_ID` eintragen. In `.xcconfig`-Dateien beginnt `//` einen Kommentar, URLs werden deshalb als `http:/$()/host:8080/api` geschrieben.
2. `ios/labs-101.xcodeproj` in Xcode öffnen und starten – oder per Kommandozeile:

```sh
cd ios

# Simulator
xcodebuild -project labs-101.xcodeproj -scheme labs-101 \
  -destination 'platform=iOS Simulator,OS=26.5,name=iPhone 17 Pro' \
  -configuration Debug -derivedDataPath ./build/labs101-dd build

# Angeschlossenes iPhone (IDs über `xcrun xctrace list devices` bzw. `xcrun devicectl list devices`)
xcodebuild -project labs-101.xcodeproj -scheme labs-101 \
  -destination 'platform=iOS,id=<XCODE_DEVICE_ID>' \
  -configuration Debug -derivedDataPath ./build/labs101-dev \
  -allowProvisioningUpdates build
xcrun devicectl device install app \
  --device <DEVICECTL_DEVICE_ID> \
  ./build/labs101-dev/Build/Products/Debug-iphoneos/labs-101.app
```

## Daten importieren

Die Rohdaten liegen in `data/` (git-ignoriert), die Import-Skripte in [`scripts/`](scripts/).

| Skript | Zweck |
| --- | --- |
| `import_BLS_data.py` | Importiert den Bundeslebensmittelschlüssel (Excel) über die Backend-API. Benötigt `pandas`, `openpyxl`, `requests`. |
| `off_copy_import.py` | Streamt den Open-Food-Facts-CSV-Export (~6 GB) per `COPY` direkt in die Tabelle `open_food`. Benötigt `psycopg2-binary`. |
| `seed_exercises.sql` | Grundstock an Übungen inkl. Muskelgruppen für die Muskelkarte. |

```sh
# Open Food Facts
python scripts/off_copy_import.py \
  --dsn "postgresql://user:abc123@localhost:5432/labs101" \
  --file data/en.openfoodfacts.org.products.csv

# Übungen
docker exec -i postgres-labs-101 psql -U user -d labs101 < scripts/seed_exercises.sql
```

## API

Alle Endpunkte liegen unter `/api` und verlangen den Header `X-API-Key`. Die wichtigsten Ressourcen:

| Pfad | Inhalt |
| --- | --- |
| `/api/foods` | Lebensmittel, Suche, Barcode, Open Food Facts, Text-Extraktion, Tracking |
| `/api/workouts` | Workout-Vorlagen und Übungen |
| `/api/calendar` | Kalendereinträge des Planers |
| `/api/users/{userId}/tracked-foods` | Getrackte Mahlzeiten |
| `/api/users/{userId}/workout-sessions` | Aktive und abgeschlossene Trainings |
| `/api/users/{userId}/runs` | Ausgewertete Läufe |
| `/api/users/{userId}/health` | Health-Sync: Samples, Characteristics, Schreibaufträge |
| `/api/users/{userId}/health/overview` | Kennzahlen für die Startseite |
| `/api/users/{userId}/settings` | Nutzereinstellungen |

Eine fertige Request-Sammlung für [Bruno](https://www.usebruno.com/) liegt in [`bruno-labs-101/`](bruno-labs-101/) (Environments `development` und `production`).

## Projektstruktur

```
.
├── backend/            Spring-Boot-API
├── frontend/           Next.js-Web-App
├── ios/                SwiftUI-App mit HealthKit-Sync
├── bruno-labs-101/     API-Requests für Bruno
├── documents/          Architektur-Doku (Health-Sync, DB-Diagramme)
├── scripts/            Datenimporte und Seeds
├── data/               Rohdaten für Importe (nicht im Repo)
└── docker-compose.yml  DB, Backend und Frontend
```

## CI/CD

- **Test And Build** ([`.github/workflows/test-build.yaml`](.github/workflows/test-build.yaml)) – bei jedem Pull Request: `mvn verify` für das Backend gegen eine Postgres aus Docker, `npm run build` für das Frontend.
- **Deploy to server** ([`.github/workflows/deployment.yaml`](.github/workflows/deployment.yaml)) – bei Push auf `main`: `docker compose up -d --build` auf einem Self-hosted Runner, die Secrets kommen aus dem GitHub-Environment `production`.
