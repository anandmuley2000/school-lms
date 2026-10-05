# Railway deployment

This repository is a monorepo, but the API image is built from the repository root. The root Dockerfile copies `backend/src` and `backend/pom.xml` alongside the root Maven wrapper files. Setting the service root to `/backend` changes the build context and makes those root-relative Docker `COPY` sources unavailable.

## Exact Railway service settings

This project uses three existing Railway services: `courteous-truth` for the Spring Boot API, `MySQL` for the database, and `school-lms` for the React admin frontend. The root [`railway.json`](railway.json) selects the backend Dockerfile; the frontend must use [`admin/railway.json`](admin/railway.json). Keep these service roots and health checks separate. Public service domains are assigned in the Railway dashboard and are not configured in this repository.

Configure the existing API service (do not create a second project or service):

| Setting | Value |
| --- | --- |
| Source repository | This repository and the branch you deploy |
| Root Directory | `/` (repository root; do not use `/backend`) |
| Config as Code / Config File Path | `/railway.json` |
| Builder | Dockerfile (`DOCKERFILE`) |
| Dockerfile Path | `Dockerfile` at the repository root |
| Container port | `8080`; the application listens on Railway's injected `PORT`, defaulting to `8080` |
| Healthcheck Path | `/actuator/health` |
| Healthcheck Timeout | `300` seconds |

The checked-in `railway.json` already selects the root Dockerfile and configures the health check and restart policy. Railway's Config File Path is an absolute repository path and is independent of Root Directory. If the dashboard currently points at `/backend/railway.json`, change it to `/railway.json`. The Railway config file does not choose the service Root Directory, so set `/` separately in the dashboard.

## Required API service variables

Set these in the Railway service's Variables tab. Keep all passwords and secrets in Railway Variables; do not put actual values in Git.

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_HOST` | `${{MySQL.MYSQLHOST}}` |
| `DB_PORT` | `${{MySQL.MYSQLPORT}}` |
| `DB_NAME` | `${{MySQL.MYSQLDATABASE}}` |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `DB_SSL_MODE` | `REQUIRED` when supported by the database provider |
| `JWT_SECRET` | Random secret of at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact HTTPS origins for deployed web clients, without paths or wildcards |
| `DEMO_DATA_ENABLED` | `false` |

Railway variable references use the MySQL service name exactly as shown in the project. The expressions above assume its service is named `MySQL`; if the dashboard label differs, replace `MySQL` in each reference with that exact service name. Add `CORS_ALLOWED_ORIGINS` after confirming the actual frontend domain. If that domain is `https://courteous-truth-production-470f.up.railway.app`, use that origin only when the dashboard confirms it is the frontend; do not set the API's own domain as a browser origin by assumption.

Railway supplies `PORT` to the service. The production Spring configuration binds to `0.0.0.0` and uses `${PORT:8080}`. Do not set `PORT` to a value that conflicts with Railway's assigned port. `EXPOSE 8080` documents the app's fallback/container port; Railway routes to the runtime `PORT`.

The production datasource reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_SSL_MODE`. Production schema validation remains enabled (`ddl-auto=validate`); demo data and reset endpoints remain disabled by default.

## Database migration

1. Provision or select the production MySQL 8 database and verify the API service can reach its host and port.
2. Take a database backup before changing schema.
3. Apply each numbered SQL migration in `backend/schema/migrations` in filename order using the database provider's SQL console or a MySQL client: `V2__timetable_leave_and_bus.sql`, `V3__explicit_event_holidays.sql`, then `V4__leave_reviewer_name.sql`, each against `DB_NAME`. They are also packaged in the API JAR under `BOOT-INF/classes/db/migration/`.
4. Run the scripts against a backup-confirmed database. V2 is safe to re-run after a partial application and adds fields/tables without deleting student or parent records. V3 adds the explicit holiday flag while preserving existing events (they default to non-holiday until an admin marks them). Migrations are not automatically applied at API startup; the API validates the schema and fails startup when migrations are missing.
5. Do not run development seed fixtures against production. Production has no first-admin bootstrap endpoint; provision the first ADMIN account through an audited DBA operation using a BCrypt password hash.

## Deploy or redeploy the existing service

1. Commit and push the intended changes to the branch connected to the existing Railway service.
2. In the Railway dashboard, open the existing API service and set Root Directory to `/`.
3. Set Config File Path to `/railway.json`; set the Dockerfile path to `Dockerfile` if the dashboard shows that field. Ensure the builder is Dockerfile.
4. Add/verify the production variables above and the MySQL connection values. Do not paste secrets into the repository or deployment logs.
5. Trigger a deployment from the latest commit (or use **Redeploy** after saving settings).
6. Inspect build logs. Railway should use the root `Dockerfile`; Docker `COPY backend/...` should resolve from the repository root.
7. Wait for `/actuator/health` to report healthy, then verify the deployed API at `https://YOUR_API_HOST/actuator/health` and the API base URL `https://YOUR_API_HOST/api`.

## Web admin and mobile clients

For the existing Railway frontend service `school-lms`, set **Root Directory** to `/admin`, **Config File Path** to `/admin/railway.json`, and **Builder** to Railpack. Its build command is `npm ci --include=dev && npm run build`; its start command serves `dist` on `0.0.0.0:$PORT`, and its health check is `/` (never `/actuator/health`). Set its build variable `VITE_API_URL` to the actual `courteous-truth` backend HTTPS URL ending in `/api` (the older `VITE_API_BASE_URL` name remains accepted for compatibility). Add the frontend's exact HTTPS origin to the backend's `CORS_ALLOWED_ORIGINS`. Local development remains independent and may use a local API; a production HTTPS URL is not a prerequisite for the local development build.

The Expo app reads `EXPO_PUBLIC_API_BASE_URL` at build time (or `expo.extra.apiBaseUrl` from its app config). For a production mobile build set it to the actual `https://YOUR-BACKEND-DOMAIN/api`, set `APP_ENV=production`, and build from `mobile/`. Keep Android signing files and passwords in local Gradle properties or CI secrets. The repository does not contain a production signing key.

## Release verification

After deployment, verify `/actuator/health`, sign in with a non-demo production account, and exercise the parent, teacher, and admin workflows against the production API. Test an Android device on cellular data to confirm the app does not depend on a workstation IP. This guide does not mean that a Railway deployment, domain, release keystore, or production account has already been created.
