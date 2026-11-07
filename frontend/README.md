# labs-101 frontend

Next.js web app of labs-101 (Next.js 16, React 19, Tailwind 4, shadcn). Login runs through Zitadel with
`openid-client`; all data comes from the Spring backend. Setup of the whole project, Zitadel and the
environment variables: see the [root README](../README.md).

## Development

Requires Node.js 22+, a running backend and Zitadel (see [Local development](../README.md#local-development)).

```sh
cp .env.example .env   # BACKEND_URL, AUTH_ISSUER, APP_URL, AUTH_CLIENT_ID, AUTH_CLIENT_SECRET, AUTH_SECRET, ZITADEL_ORG_ID
npm install
npm run dev            # http://localhost:3000
```

| Script | Purpose |
| --- | --- |
| `npm run dev` / `build` / `start` | Next.js dev server, production build, production server (all with webpack) |
| `npm run lint` | ESLint |
| `npm run generate:api` | Regenerates `lib/api/schema.d.ts` from the backend's OpenAPI spec [`api/openapi.json`](../api/openapi.json) |
| `npm run test:e2e` | Playwright tests in `e2e/` on a phone and a desktop viewport |

**API types:** the API client in [`lib/api/`](lib/api/) is typed from the shared contract `api/openapi.json`.
After a DTO changes in the backend, run `npm run generate:api` and commit `schema.d.ts`. CI fails if the file
is out of date.

**E2E tests:** they log in as a Zitadel test user. Put `E2E_EMAIL` and `E2E_PASSWORD` into `.env.e2e`
(git-ignored); `E2E_BASE_URL` defaults to `http://localhost:3000`.

## Structure

```
app/          routes (foods, workouts, exercises, runs, planner, friends, profile, settings, notifications, auth, sign-in)
components/   UI components (shadcn under components/ui)
lib/          API client, auth, mappers, muscle map, zustand stores
hooks/        React hooks
e2e/          Playwright tests
proxy.ts      runs before every page and server action, refreshes the Zitadel tokens in the session cookie
```

## Deployment

The [`Dockerfile`](Dockerfile) builds the image `ghcr.io/dannyyy101/labs-101-frontend`; the workflow
`images.yaml` pushes it on every change on `main`, and Argo CD deploys it with the Helm chart in
[`deploy/helm/labs-101`](../deploy/helm/labs-101).
