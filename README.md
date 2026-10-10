# labs-101

Persönliche Health- & Fitness-Plattform: Ernährung tracken, Workouts planen und loggen, Läufe auswerten und Apple-Health-Daten zentral speichern.

Das Projekt besteht aus drei Clients/Diensten und dem Identity Provider Zitadel, die sich eine Postgres-Instanz teilen:

| Teil | Technik | Aufgabe |
| --- | --- | --- |
| [`backend/`](backend/) | Spring Boot 4 · Java 25 · JPA · Flyway | REST-API unter `/api`, Geschäftslogik, Datenbank-Migrationen |
| [`frontend/`](frontend/) | Next.js 16 · React 19 · Tailwind 4 · shadcn · openid-client | Web-App mit Login, Dashboards und Formularen |
| [`ios/`](ios/) | SwiftUI · HealthKit (iOS 26.5) | Apple-Health-Sync, Essen- und Workout-Tracking unterwegs |
| Zitadel | Docker (v4) | Identity Provider unter `auth.project101.tech`: Login mit Passwort, Passkey oder Google |
| Postgres | Docker | Datenbanken `labs101` (App-Daten) und `zitadel` |

```mermaid
flowchart LR
    iOS["iOS-App<br/>(HealthKit)"] -- "Login (PKCE)" --> IdP["Zitadel<br/>:8081"]
    Web["Next.js-Frontend<br/>:3000"] -- "Login (PKCE)" --> IdP
    iOS -- "Bearer-Token" --> API["Spring-Backend<br/>:8080/api"]
    Web -- "Bearer-Token" --> API
    API -- "prüft Token (JWKS)" --> IdP
    API --> DB[("Postgres<br/>labs101")]
    IdP --> ZDB[("Postgres<br/>zitadel")]
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
- Zitadel: <http://localhost:8081/ui/console> (Einrichtung siehe [Authentifizierung](#authentifizierung-zitadel))
- Postgres: `localhost:5432`, Datenbank `labs101`

Die Datenbank-Migrationen (Flyway, [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration)) laufen beim Start des Backends automatisch.

### Umgebungsvariablen

Die `.env` im Repo-Root wird von Docker Compose und der VS-Code-Launch-Config des Backends gelesen.

| Variable | Genutzt von | Beschreibung |
| --- | --- | --- |
| `POSTGRES_USERNAME` / `POSTGRES_PASSWORD` | DB, Backend, Zitadel | Zugangsdaten der Datenbank, Zitadel legt damit beim ersten Start seine eigene Datenbank an |
| `ZITADEL_EXTERNALDOMAIN` / `ZITADEL_EXTERNALPORT` / `ZITADEL_EXTERNALSECURE` | Zitadel | Öffentliche Adresse, Produktion: `auth.project101.tech`, `443`, `true` |
| `ZITADEL_TLS_MODE` | Zitadel | `external` hinter dem Reverse Proxy (Produktion), `disabled` lokal |
| `ZITADEL_MASTERKEY` | Zitadel | Genau 32 Zeichen, verschlüsselt die Secrets in der Zitadel-Datenbank. **Nie mehr ändern.** |
| `ZITADEL_DB_PASSWORD` | Zitadel | Passwort des Datenbank-Users `zitadel` |
| `ZITADEL_ADMIN_USERNAME` / `ZITADEL_ADMIN_PASSWORD` | Zitadel | Erster Admin, nur beim allerersten Start verwendet |
| `AUTH_ISSUER` | Backend, Frontend | Öffentliche URL von Zitadel, muss dem `iss` der Tokens entsprechen |
| `AUTH_AUDIENCE` | Backend | Optional: Projekt-ID in Zitadel, Tokens anderer Projekte werden dann abgelehnt |
| `APP_URL` | Frontend | Öffentliche URL des Frontends, Zitadel leitet nach `${APP_URL}/auth/callback` zurück |
| `AUTH_CLIENT_ID` / `AUTH_CLIENT_SECRET` | Frontend | Web-App in Zitadel |
| `AUTH_SECRET` | Frontend | Mindestens 32 zufällige Zeichen, verschlüsselt das Session-Cookie |

## Lokale Entwicklung

Für die Entwicklung reicht es, nur die Datenbank in Docker zu starten:

```sh
docker compose up -d db
```

### Backend

Benötigt JDK 25.

```sh
cd backend
AUTH_ISSUER=http://localhost:8081 ./mvnw spring-boot:run
```

Die Standard-Verbindung (`localhost:5432`, `user` / `abc123`) steht in [`application.properties`](backend/src/main/resources/application.properties) und passt zu `.env.example`. Alternativ in VS Code die Launch-Config „Spring Boot-BackendApplication“ starten – sie lädt die `.env` automatisch.

Tests (u. a. mit Embedded Postgres):

```sh
cd backend
./mvnw verify
```

### Frontend

Benötigt Node.js 22+. `AUTH_ISSUER`, `APP_URL`, `AUTH_CLIENT_ID`, `AUTH_CLIENT_SECRET`, `AUTH_SECRET` und `BACKEND_URL=http://localhost:8080/api` gehören in `frontend/.env`, Vorlage: `cp frontend/.env.example frontend/.env`.

```sh
cd frontend
npm install
npm run dev     # http://localhost:3000
npm run lint
npm run build
```

Lokal läuft Zitadel per `docker compose up -d db zitadel` auf <http://localhost:8081>, Backend und Frontend direkt auf dem Rechner. Ein Backend im Docker-Netz kann `localhost:8081` nicht erreichen, deshalb lokal nicht alles in Docker starten.

### iOS-App

Benötigt Xcode 26 und ein Gerät bzw. einen Simulator mit iOS 26.5.

1. Secrets anlegen:
   ```sh
   cp ios/Config/Secrets.example.xcconfig ios/Config/Secrets.xcconfig
   ```
   und `API_BASE_URL`, `AUTH_ISSUER` und `AUTH_CLIENT_ID` (native App in Zitadel) eintragen. Die App meldet sich beim Start über die Login-Seite von Zitadel an, Tokens liegen in der Keychain. In `.xcconfig`-Dateien beginnt `//` einen Kommentar, URLs werden deshalb als `http:/$()/host:8080/api` geschrieben.
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
| `import_BLS_data.py` | Importiert den Bundeslebensmittelschlüssel (Excel) über die Backend-API, das Access-Token kommt aus `ACCESS_TOKEN`. Benötigt `pandas`, `openpyxl`, `requests`. |
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

Alle Endpunkte liegen unter `/api` und verlangen ein Access-Token von Zitadel im Header `Authorization: Bearer …`. Der Nutzer kommt aus dem Token (`sub`), nutzerbezogene Pfade beginnen deshalb mit `/api/users/me`. Beim ersten Request legt das Backend den Nutzer in der Tabelle `user` an. Die wichtigsten Ressourcen:

| Pfad | Inhalt |
| --- | --- |
| `/api/foods` | Lebensmittel, Suche, Barcode, Open Food Facts, Text-Extraktion, Tracking |
| `/api/workouts` | Workout-Vorlagen und Übungen |
| `/api/calendar` | Kalendereinträge des Planers |
| `/api/users/me/tracked-foods` | Getrackte Mahlzeiten |
| `/api/users/me/workout-sessions` | Aktive und abgeschlossene Trainings |
| `/api/users/me/runs` | Ausgewertete Läufe |
| `/api/users/me/health` | Health-Sync: Samples, Characteristics, Schreibaufträge |
| `/api/users/me/health/overview` | Kennzahlen für die Startseite |
| `/api/users/me/settings` | Nutzereinstellungen |

Eine fertige Request-Sammlung für [Bruno](https://www.usebruno.com/) liegt in [`bruno-labs-101/`](bruno-labs-101/) (Environments `development` und `production`). Das Token kommt in die Environment-Variable `access_token`, z. B. von einem Service-User in Zitadel (Token-Typ JWT) per Client Credentials:

```sh
curl -s -u '<client id>:<client secret>' -d 'grant_type=client_credentials&scope=openid' \
  https://auth.project101.tech/oauth/v2/token | jq -r .access_token
```

## Authentifizierung (Zitadel)

Einmalige Einrichtung in der Zitadel-Console (`/ui/console`, Login mit `ZITADEL_ADMIN_USERNAME@labs-101.<domain>`, das Passwort muss beim ersten Login geändert werden):

1. **Projekt** `labs-101` anlegen.
2. **Web-App** im Projekt: Typ *Web*, Authentifizierung *Code* (Client Secret), Redirect-URI `https://project101.tech/auth/callback`, Post-Logout-URI `https://project101.tech/sign-in` (lokal `http://localhost:3000/…`, dafür *Development Mode* aktivieren). Client-ID und Secret → `AUTH_CLIENT_ID` / `AUTH_CLIENT_SECRET`.
3. **Native App** im Projekt: Typ *Native*, Authentifizierung *PKCE*, Redirect-URI `labs101://auth/callback`. Client-ID → `AUTH_CLIENT_ID` in `ios/Config/Secrets.xcconfig`.
4. Bei **beiden Apps** unter *Token Settings* den **Auth Token Type auf JWT** stellen und *Refresh Token* erlauben. Das Backend prüft die Tokens nur über die Schlüssel von Zitadel und kann mit undurchsichtigen Tokens nichts anfangen.
5. **Google** (#82): unter *Settings → Identity Providers* Google mit Client-ID/Secret aus der Google Cloud Console anlegen (Redirect-URI zeigt Zitadel an) und in den *Login Settings* aktivieren.
6. **Registrierung abschalten** (*Login Settings → Register allowed* aus) und Nutzer selbst anlegen bzw. einladen. Das ersetzt die frühere Allowlist `ALLOWED_EMAIL_ADDRESSES`. Für Google-Logins *Account creation* beim Identity Provider aus und *Account linking* per E-Mail an lassen.

Produktion: Der Reverse Proxy leitet `auth.project101.tech` per HTTP/2 (h2c) auf Port `8081` weiter. Zitadel braucht dafür `ZITADEL_TLS_MODE=external`.

### Umstieg von Better Auth

Bestehende Daten hängen an den alten Better-Auth-IDs. Für jeden Nutzer nach dem Anlegen in Zitadel einmal (die Zitadel-ID steht in der Console beim User):

```sh
docker exec -i postgres-labs-101 psql -U user -d labs101 \
  -c "SELECT move_user(id, '<zitadel user id>') FROM \"user\" WHERE email = 'max@example.com' AND id <> '<zitadel user id>'"
```

`move_user` (Flyway-Migration V5) zieht alle Daten über die Foreign Keys auf die neue ID um, auch wenn sich der Nutzer schon neu angemeldet hat. Danach entfernt [`scripts/drop_better_auth_tables.sql`](scripts/drop_better_auth_tables.sql) die alten Tabellen `session`, `account` und `verification`. Das Skript bricht ab, solange noch Nutzer mit Better-Auth-ID existieren.

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
