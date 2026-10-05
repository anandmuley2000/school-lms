# Railway deployment

This repository is a monorepo, but the API image is built from the repository root. The root Dockerfile copies `backend/src` and `backend/pom.xml` alongside the root Maven wrapper files. Setting the service root to `/backend` changes the build context and makes those root-relative Docker `COPY` sources unavailable.

## Exact Railway service settings

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
| `DB_HOST` | Reachable MySQL hostname (not `localhost`) |
| `DB_PORT` | MySQL port, commonly `3306` |
| `DB_NAME` | Database/schema name |
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `DB_SSL_MODE` | `REQUIRED` when supported by the database provider |
| `JWT_SECRET` | Random secret of at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact HTTPS origins for deployed web clients |
| `DEMO_DATA_ENABLED` | `false` |

Railway supplies `PORT` to the service. The production Spring configuration binds to `0.0.0.0` and uses `${PORT:8080}`. Do not set `PORT` to a value that conflicts with Railway's assigned port. `EXPOSE 8080` documents the app's fallback/container port; Railway routes to the runtime `PORT`.

The production datasource reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_SSL_MODE`. Production schema validation remains enabled (`ddl-auto=validate`); demo data and reset endpoints remain disabled by default.

## Database migration

1. Provision or select the production MySQL 8 database and verify the API service can reach its host and port.
2. Take a database backup before changing schema.
3. Apply each numbered SQL migration in `backend/schema/migrations` in filename order using the database provider's SQL console or a MySQL client. For the current migration, run `backend/schema/migrations/V2__timetable_leave_and_bus.sql` against `DB_NAME`.
4. Confirm the SQL migration completed successfully before deploying. The API intentionally fails startup when the schema does not match the entities.
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

Build and host the existing Vite admin app separately as a static site. Set `VITE_API_BASE_URL=https://YOUR_API_HOST/api` at production build time and add the static site's exact HTTPS origin to `CORS_ALLOWED_ORIGINS`. Local development remains independent and may use a local API; a production HTTPS URL is not required to run the local development build.

The Expo app reads `EXPO_PUBLIC_API_BASE_URL` at build time (or `expo.extra.apiBaseUrl` from its app config). For a production mobile build use `https://YOUR_API_HOST/api`, set `APP_ENV=production`, and build from `mobile/`. Keep Android signing files and passwords in local Gradle properties or CI secrets. The repository does not contain a production signing key.

## Release verification

After deployment, verify `/actuator/health`, sign in with a non-demo production account, and exercise the parent, teacher, and admin workflows against the production API. Test an Android device on cellular data to confirm the app does not depend on a workstation IP. This guide does not mean that a Railway deployment, domain, release keystore, or production account has already been created.
