# labs-101

Personal health & fitness platform: track nutrition, plan and log workouts, analyze runs and store Apple Health data in one place.

The project consists of three clients/services and the identity provider Zitadel, all sharing one Postgres instance:

| Part | Tech | Purpose |
| --- | --- | --- |
| [`backend/`](backend/) | Spring Boot 4 · Java 25 · JPA · Flyway | REST API under `/api`, business logic, database migrations |
| [`frontend/`](frontend/) | Next.js 16 · React 19 · Tailwind 4 · shadcn · openid-client | Web app with login, dashboards and forms |
| [`ios/`](ios/) | SwiftUI · HealthKit (iOS 26.5) | Apple Health sync, food and workout tracking on the go |
| Zitadel | Docker (v4) locally, Helm chart `helm/auth` in the infra repo in production | Shared identity provider for all apps at `auth.project101.tech`: login with password, passkey or Google |
| Postgres | Docker | Database `labs101` (app data), locally also `zitadel` |

```mermaid
flowchart LR
    iOS["iOS app<br/>(HealthKit)"] -- "Login (PKCE)" --> IdP["Zitadel<br/>:8081"]
    Web["Next.js frontend<br/>:3000"] -- "Login (PKCE)" --> IdP
    iOS -- "Bearer token" --> API["Spring backend<br/>:8080/api"]
    Web -- "Bearer token" --> API
    API -- "verifies token (JWKS)" --> IdP
    API --> DB[("Postgres<br/>labs101")]
    IdP --> ZDB[("Postgres<br/>zitadel")]
```

## Features

- **Nutrition** – search foods (trigram full-text search), scan barcodes, extract foods from free text and track them with portions. Data sources: Bundeslebensmittelschlüssel (BLS, the German food composition database) and Open Food Facts.
- **Workouts** – exercises with trained muscles, workout templates, live sessions, statistics and a muscle map.
- **Runs** – analysis of runs synced from Apple Health, including the GPS route on a map.
- **Apple Health sync** – the iOS app mirrors HealthKit samples to the backend; in the other direction, the backend can queue write requests. Details: [`documents/health-sync.md`](documents/health-sync.md).
- **Health overview & planner** – home page with key metrics, calendar for planned sessions.
- **Friends** – send and accept friend requests by name/email, view friends' profiles (streak, heatmap, achievements), see who is currently training and cheer them on once per workout (the friend gets a notification).
- **Settings** – per-user settings.
- **Claude (MCP)** – the backend is an MCP server; Claude signs in via Zitadel, can analyze workouts, runs, health data, nutrition and the calendar, and can track meals, create workout templates and plan events, see [Connecting Claude](#connecting-claude-mcp).
- **Notifications (iOS)** – local, checked after every health sync: the sleep summary in the morning, statistics after a workout, a reminder at 8 pm if ≥ 500 kcal are still left, plus the backend's unread notifications (the bell in the web app). Can be turned off in the Health tab; debug builds have a test button there.

## Quick start with Docker

Requirement: Docker with Compose.

```sh
cp .env.example .env   # and fill in the values, see below
docker compose --profile local up -d --build
```

Zitadel belongs to the Compose profile `local`. Without `--profile local` it does not start, just like on the server, where the shared Zitadel from the infra repo runs.

Afterwards these are running:

- Frontend: <http://localhost:3000>
- Backend: <http://localhost:8080/api>
- Zitadel: <http://localhost:8081/ui/console> (setup see [Authentication](#authentication-zitadel))
- Postgres: `localhost:5432`, database `labs101`

The database migrations (Flyway, [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration)) run automatically when the backend starts.

### Environment variables

The `.env` in the repo root is read by Docker Compose and by the backend's VS Code launch config.

| Variable | Used by | Description |
| --- | --- | --- |
| `POSTGRES_USERNAME` / `POSTGRES_PASSWORD` | DB, backend, Zitadel | Database credentials; Zitadel uses them to create its own database on first start |
| `ZITADEL_EXTERNALDOMAIN` / `ZITADEL_EXTERNALPORT` / `ZITADEL_EXTERNALSECURE` | Zitadel | Public address, production: `auth.project101.tech`, `443`, `true` |
| `ZITADEL_TLS_MODE` | Zitadel | `external` behind the reverse proxy (production), `disabled` locally |
| `ZITADEL_MASTERKEY` | Zitadel | Exactly 32 characters, encrypts the secrets in the Zitadel database. **Never change it.** |
| `ZITADEL_DB_PASSWORD` | Zitadel | Password of the database user `zitadel` |
| `ZITADEL_ADMIN_USERNAME` / `ZITADEL_ADMIN_PASSWORD` | Zitadel | First admin, only used on the very first start |
| `AUTH_ISSUER` | Backend, frontend | Public URL of Zitadel, must match the `iss` of the tokens |
| `AUTH_AUDIENCE` | Backend | Optional: project ID in Zitadel; tokens from other projects are then rejected |
| `ZITADEL_ORG_ID` | Backend, frontend | ID of the users' organization in Zitadel, used to build the profile picture URLs. Empty: no pictures, initials only |
| `APP_URL` | Frontend | Public URL of the frontend; Zitadel redirects back to `${APP_URL}/auth/callback` |
| `AUTH_CLIENT_ID` / `AUTH_CLIENT_SECRET` | Frontend | Web app in Zitadel |
| `AUTH_SECRET` | Frontend | At least 32 random characters, encrypts the session cookie |
| `MCP_RESOURCE_URL` | Backend | Public URL of the MCP server, exactly the address entered in Claude. Production: `https://api.project101.tech/api/mcp` |

## Local development

For development it is enough to run only the database in Docker:

```sh
docker compose up -d db
```

### Backend

Requires JDK 25.

```sh
cd backend
AUTH_ISSUER=http://localhost:8081 ./mvnw spring-boot:run
```

The default connection (`localhost:5432`, `user` / `abc123`) is set in [`application.properties`](backend/src/main/resources/application.properties) and matches `.env.example`. Alternatively, start the VS Code launch config "Spring Boot-BackendApplication" – it loads the `.env` automatically.

Tests (including embedded Postgres):

```sh
cd backend
./mvnw verify
```

### Frontend

Requires Node.js 22+. `AUTH_ISSUER`, `APP_URL`, `AUTH_CLIENT_ID`, `AUTH_CLIENT_SECRET`, `AUTH_SECRET` and `BACKEND_URL=http://localhost:8080/api` go into `frontend/.env`, template: `cp frontend/.env.example frontend/.env`.

```sh
cd frontend
npm install
npm run dev     # http://localhost:3000
npm run lint
npm run build
```

Locally, Zitadel runs via `docker compose up -d db zitadel` on <http://localhost:8081>, while backend and frontend run directly on the machine. A backend inside the Docker network cannot reach `localhost:8081`, so don't run everything in Docker locally.

### iOS app

Requires Xcode 26 and a device or simulator with iOS 26.5.

1. Create the secrets:
   ```sh
   cp ios/Config/Secrets.example.xcconfig ios/Config/Secrets.xcconfig
   ```
   and fill in `API_BASE_URL`, `AUTH_ISSUER` and `AUTH_CLIENT_ID` (native app in Zitadel). On launch the app signs in through Zitadel's login page; tokens are stored in the Keychain. In `.xcconfig` files `//` starts a comment, so URLs are written as `http:/$()/host:8080/api`.
2. Open `ios/labs-101.xcodeproj` in Xcode and run it – or from the command line:

```sh
cd ios

# Simulator
xcodebuild -project labs-101.xcodeproj -scheme labs-101 \
  -destination 'platform=iOS Simulator,OS=26.5,name=iPhone 17 Pro' \
  -configuration Debug -derivedDataPath ./build/labs101-dd build

# Connected iPhone (IDs via `xcrun xctrace list devices` or `xcrun devicectl list devices`)
xcodebuild -project labs-101.xcodeproj -scheme labs-101 \
  -destination 'platform=iOS,id=<XCODE_DEVICE_ID>' \
  -configuration Debug -derivedDataPath ./build/labs101-dev \
  -allowProvisioningUpdates build
xcrun devicectl device install app \
  --device <DEVICECTL_DEVICE_ID> \
  ./build/labs101-dev/Build/Products/Debug-iphoneos/labs-101.app
```

## Importing data

The raw data lives in `data/` (git-ignored), the import scripts in [`scripts/`](scripts/).

| Script | Purpose |
| --- | --- |
| `import_BLS_data.py` | Imports the Bundeslebensmittelschlüssel (Excel) through the backend API; the access token comes from `ACCESS_TOKEN`. Requires `pandas`, `openpyxl`, `requests`. |
| `off_copy_import.py` | Streams the Open Food Facts CSV export (~6 GB) via `COPY` directly into the `open_food` table. Requires `psycopg2-binary`. |
| `seed_exercises.sql` | Base set of exercises including muscle groups for the muscle map. |

```sh
# Open Food Facts
python scripts/off_copy_import.py \
  --dsn "postgresql://user:abc123@localhost:5432/labs101" \
  --file data/en.openfoodfacts.org.products.csv

# Exercises
docker exec -i postgres-labs-101 psql -U user -d labs101 < scripts/seed_exercises.sql
```

## API

All endpoints live under `/api` and require a Zitadel access token in the `Authorization: Bearer …` header. The user is taken from the token (`sub`), so user-specific paths start with `/api/users/me`. On the first request the backend creates the user in the `user` table. The main resources:

| Path | Content |
| --- | --- |
| `/api/foods` | Foods, search, barcode, Open Food Facts, text extraction, tracking |
| `/api/workouts` | Workout templates and exercises |
| `/api/calendar` | Planner calendar entries |
| `/api/users/me/tracked-foods` | Tracked meals |
| `/api/users/me/workout-sessions` | Active and completed workouts |
| `/api/users/me/runs` | Analyzed runs |
| `/api/users/me/health` | Health sync: samples, characteristics, write requests |
| `/api/users/me/health/overview` | Key metrics for the home page |
| `/api/users/me/settings` | User settings |

A ready-made request collection for [Bruno](https://www.usebruno.com/) is in [`bruno-labs-101/`](bruno-labs-101/) (environments `development` and `production`). The token goes into the environment variable `access_token`, e.g. from a service user in Zitadel (token type JWT) via client credentials:

```sh
curl -s -u '<client id>:<client secret>' -d 'grant_type=client_credentials&scope=openid' \
  https://auth.project101.tech/oauth/v2/token | jq -r .access_token
```

## Connecting Claude (MCP)

The backend is an [MCP server](https://modelcontextprotocol.io) (Streamable HTTP) at `/api/mcp`, code in [`backend/…/mcp`](backend/src/main/java/com/labs_101/backend/mcp). Claude signs in through Zitadel just like the apps, and everyone only sees their own data. All tools are read-only except the ones marked **writes**, which track food, create workout templates and plan calendar events:

| Tool | Content |
| --- | --- |
| `get_profile` | Streak, active days, achievements, calorie goal |
| `get_daily_health_overview` | Sleep, recovery, strain, activity, vitals for one day |
| `list_health_data_types` / `get_health_samples` | Synced Apple Health types and their raw data |
| `list_workouts` / `list_workout_templates` | Completed workouts with sets and notes, workout plans |
| `list_exercises` | Exercises with their ID and type (strength or cardio), for `create_workout_template` |
| `create_workout_template` | **writes:** create a workout template from exercises with sets (strength) or minutes (cardio) |
| `list_runs` / `get_run` | Runs with splits, heart rate and route |
| `get_nutrition` | Tracked meals per day (max. 31 days) |
| `search_foods` | Search foods, with nutrients per 100 g, portions and the last tracked amount |
| `track_food` | **writes:** track a food in grams or portions for a meal, also for a past day |
| `delete_tracked_food` | **writes:** delete one of your own tracked entries |
| `list_calendar_events` | Planned events (default: the next 14 days) |
| `create_calendar_event` | **writes:** plan an event, optionally with a workout template and a daily/weekly/monthly recurrence |

Without a token, `/api/mcp` responds with `401` and `WWW-Authenticate: Bearer resource_metadata="…"`; `/.well-known/oauth-protected-resource/api/mcp` states that Zitadel issues the tokens (RFC 9728). Claude requests do not count as a login for the streak.

Setup:

1. In Zitadel, create a **web app** `Claude` in the `labs-101` project: authentication *Code* (client secret), redirect URI `https://claude.ai/api/mcp/auth_callback`. Under *Token Settings*, enable **Auth Token Type JWT** and *Refresh Token*.
2. In Claude, under *Settings → Connectors → Add custom connector*, enter the URL `https://api.project101.tech/api/mcp` (must match `MCP_RESOURCE_URL` exactly) and, under *Advanced settings*, the client ID and client secret of the app from step 1.
3. *Connect* – Zitadel's login opens; afterwards Claude can answer questions like "How has my bench press developed over the last 3 months?".

Zitadel does not support Dynamic Client Registration, hence the dedicated app with client ID and secret. For Claude Code, the app would additionally need the loopback redirects `http://localhost/callback` and `http://127.0.0.1/callback` with any port.

## Authentication (Zitadel)

One-time setup in the Zitadel console (`/ui/console`, log in with `ZITADEL_ADMIN_USERNAME@labs-101.<domain>`; the password must be changed on first login):

1. Create the **project** `labs-101`.
2. **Web app** in the project: type *Web*, authentication *Code* (client secret), redirect URI `https://project101.tech/auth/callback`, post-logout URI `https://project101.tech/sign-in` (locally `http://localhost:3000/…`, enable *Development Mode* for that). Client ID and secret → `AUTH_CLIENT_ID` / `AUTH_CLIENT_SECRET`.
3. **Native app** in the project: type *Native*, authentication *PKCE*, redirect URI `labs101://auth/callback`. Client ID → `AUTH_CLIENT_ID` in `ios/Config/Secrets.xcconfig`.
4. For **both apps**, set the **Auth Token Type to JWT** under *Token Settings* and allow *Refresh Token*. The backend validates tokens only against Zitadel's keys and cannot handle opaque tokens.
5. **Google** (#82): under *Settings → Identity Providers*, add Google with the client ID/secret from the Google Cloud Console (Zitadel shows the redirect URI) and enable it in the *Login Settings*.
6. **Profile pictures:** enter the ID of the organization the users belong to (console → *Organization*, shown next to the name) as `ZITADEL_ORG_ID` for backend and frontend.
7. **Disable registration** (*Login Settings → Register allowed* off) and create or invite users yourself. This replaces the former allowlist `ALLOWED_EMAIL_ADDRESSES`. For Google logins, turn *Account creation* off at the identity provider and leave *Account linking* by email on.

**Profiles** (name, email, profile picture) are maintained by each user in Zitadel at `/ui/console/users/me`; web and iOS link there. The backend does not store a copy. Like on GitHub, profile pictures have a fixed URL per user, `${AUTH_ISSUER}/assets/v1/${ZITADEL_ORG_ID}/users/<user id>/avatar`, so a new picture shows up there without any syncing. Without a picture Zitadel responds with 404 and the apps show the initial instead. Upload pictures as JPG or PNG; Chrome and Firefox don't display HEIC (iPhone photos). The backend fetches the user's own profile (`GET /api/users/me`) from the userinfo endpoint using the user's token.

Production: Zitadel no longer runs in this Compose setup but as the shared identity provider for all apps in the Kubernetes cluster (Helm chart `helm/auth` in the [`infra`](https://github.com/Dannyyy101/infra) repo). A Cloudflare Tunnel forwards `auth.project101.tech` to the service `auth-zitadel.auth.svc.cluster.local:8085`. Backend and frontend only need `AUTH_ISSUER=https://auth.project101.tech` from that.

### Migrating from Better Auth

Existing data is tied to the old Better Auth IDs. For each user, after creating them in Zitadel, run once (the Zitadel ID is shown on the user in the console):

```sh
docker exec -i postgres-labs-101 psql -U user -d labs101 \
  -c "SELECT move_user(id, '<zitadel user id>') FROM \"user\" WHERE email = 'max@example.com' AND id <> '<zitadel user id>'"
```

`move_user` (Flyway migration V5) moves all data to the new ID via the foreign keys, even if the user has already signed in again. Afterwards, [`scripts/drop_better_auth_tables.sql`](scripts/drop_better_auth_tables.sql) removes the old tables `session`, `account` and `verification`. The script aborts as long as users with a Better Auth ID still exist.

## Project structure

```
.
├── backend/            Spring Boot API
├── frontend/           Next.js web app
├── ios/                SwiftUI app with HealthKit sync
├── api/                OpenAPI spec, the shared API contract
├── deploy/helm/        Helm chart for the Kubernetes deployment
├── bruno-labs-101/     API requests for Bruno
├── documents/          Architecture docs (health sync, DB diagrams)
├── scripts/            Data imports and seeds
├── data/               Raw data for imports (not in the repo)
└── docker-compose.yml  DB, backend and frontend
```

## CI/CD

- **Test And Build** ([`.github/workflows/test-build.yaml`](.github/workflows/test-build.yaml)) – on every pull request: `mvn verify` for the backend against a Postgres in Docker, `npm run build` for the frontend. Both jobs also check that the API contract is up to date: the backend build regenerates [`api/openapi.json`](api/openapi.json), the frontend regenerates its API types from it, and the job fails if either file changed.
- **Images** ([`.github/workflows/images.yaml`](.github/workflows/images.yaml)) – on push to `main` (only when `backend/` or `frontend/` changed, or manually): builds the images of the changed components and pushes them to `ghcr.io/dannyyy101/labs-101-backend` / `-frontend`, tagged with the commit SHA and `latest`. It then writes the new tags into `values/labs-101.yaml` in the [`infra`](https://github.com/Dannyyy101/infra) repo (deploy key `INFRA_DEPLOY_KEY` from the GitHub environment `production`).
- **Deployment (GitOps)** – Argo CD watches the infra repo and deploys exactly those builds to the Kubernetes cluster with the Helm chart [`deploy/helm/labs-101`](deploy/helm/labs-101) (frontend, backend, Postgres, ingress, ServiceMonitor and the Grafana dashboard for the backend). The workflows never touch the cluster themselves. Manual installation and the required secrets are described in the [chart README](deploy/helm/labs-101/README.md).
