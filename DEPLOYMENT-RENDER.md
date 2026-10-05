# Archived deployment notes: Render

The active production target is Railway. Use [DEPLOYMENT-RAILWAY.md](DEPLOYMENT-RAILWAY.md) and the root [.env.example](.env.example) for current configuration. This file documents an earlier Render proposal and should not be used for new deployments.

# Render deployment (MySQL stays MySQL)

This guide deploys the existing React/Vite admin as a Render Static Site and the existing Spring Boot API as a Render Web Service. The API remains connected to MySQL 8 through `DB_*` environment variables. No production domain or database credentials are assumed here.

## Deployment prerequisites and cost

Render supports free static sites and free web services, but a free web service can spin down after inactivity. Render does not offer managed MySQL. Its documented self-hosted MySQL service uses a persistent disk, which is available only to paid services. A free Render API also cannot receive private network traffic. For a no-cost test, you must separately obtain a real MySQL server that is reachable from the public internet and accepts Render's outbound connection; never expose a database without provider-supported network restrictions and TLS. If you do not already have such a server, create one with a provider/account you select (possibly paid), or run Render's MySQL template and use paid Render services. Do not substitute PostgreSQL.

Render's documented MySQL template is a private Docker service with a 10 GB disk mounted at `/var/lib/mysql`; it uses MySQL 8 and is reachable as an internal host such as `mysql-service-name:3306` from services on Render's private network. The managed disk and paid service have a cost; confirm current pricing in your Render dashboard. Configure regular `mysqldump` backups. See [Render's MySQL deployment guide](https://render.com/docs/deploy-mysql), [persistent disk limits](https://render.com/docs/disks), and [free instance limitations](https://render.com/docs/free).

## 1. Prepare the existing Git repository

1. Review `git status` and commit/push the existing project to a GitHub repository you control. Do not commit `.env` files, database credentials, JWT secrets, keystores, or service account files. The repository `.gitignore` keeps local environment files and build outputs out of Git while allowing the example files.
2. In Render, connect that GitHub repository. No Render account or repository access is assumed by this guide.

## 2. Create or select a persistent MySQL 8 database

Use an existing external MySQL 8 service, or manually deploy Render's MySQL template as described in its official guide. For a Render-hosted instance, create a **Private Service** using the MySQL template, set unique `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_ROOT_PASSWORD` values, and attach a disk at exactly `/var/lib/mysql`. Never reuse the root account for the application.

Obtain these values from the database provider:

| Application variable | Value |
| --- | --- |
| `DB_HOST` | Actual hostname reachable from the backend (Render private service name when using Render's private network) |
| `DB_PORT` | MySQL port, normally `3306` |
| `DB_NAME` | Database created for this LMS |
| `DB_USERNAME` | Dedicated application account |
| `DB_PASSWORD` | That account's secret password |
| `DB_SSL_MODE` | `REQUIRED` for a TLS endpoint; choose the provider's documented value if different |

For an external database, verify it has persistent storage, TLS, and a network allow-list that permits the API's egress. Some providers require a public endpoint; some require additional CA configuration. `DB_SSL_MODE` controls the Connector/J `sslMode`, but a provider-specific CA certificate may need separate configuration. Do not enter a made-up hostname or credentials.

For a new, empty MySQL 8 database, manually apply [`backend/schema/mysql-8-schema.sql`](backend/schema/mysql-8-schema.sql) to the database before starting the production API. It contains create-only DDL for the current 12 JPA tables, no application rows, and no drop statements. It does not create the database itself. Review and back up any non-empty database first; the script is a baseline for a fresh schema, not an upgrade migration. For future entity changes, add reviewed migrations before deploying because production Hibernate only validates the schema.

## 3. Create the Render backend Web Service

1. In Render choose **New > Web Service**, select the connected repository, and use the repository root as the root directory.
2. Choose **Docker** as the runtime. The repository-root `Dockerfile` builds the existing Java 21 Spring Boot API. Keep its Docker start command. Choose a service/network plan that can reach the database (a free Render service cannot receive private-network traffic).
3. Set Health Check Path to `/actuator/health`.
4. Under Environment, add the variables below. Store `DB_PASSWORD` and `JWT_SECRET` as secrets. Generate `JWT_SECRET` with at least 32 random UTF-8 bytes and keep it stable across deploys.

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_HOST` | Real MySQL hostname |
| `DB_PORT` | Provider port, usually `3306` |
| `DB_NAME` | Real LMS database name |
| `DB_USERNAME` | Dedicated database username |
| `DB_PASSWORD` | Secret database password |
| `DB_SSL_MODE` | `REQUIRED` unless the provider documents another Connector/J mode |
| `JWT_SECRET` | Unique secret of at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Initially `https://localhost`; after creating the static site, add its exact HTTPS origin, comma-separated |
| `DEMO_RESET_ENABLED` | `false` |
| `JWT_EXPIRATION_MS` | Optional; defaults to `86400000` (one day) |
| `FILE_UPLOAD_PATH` | Optional; defaults to `/app/uploads` |

Do not set `PORT`; Render supplies it. The app listens on `${PORT:8080}` and binds to `0.0.0.0`. Production uses Hibernate `ddl-auto=validate`, disables SQL initialization and demo reset, and fails startup if required database/JWT/CORS configuration is absent. The schema is not automatically created or altered. Create the schema from a reviewed backup/migration process before deploying; do not point this at a database whose schema has not been checked.

5. Initially set `CORS_ALLOWED_ORIGINS=https://localhost` so the backend can start before the frontend URL exists. Deploy and wait for the health check to pass. Copy the actual HTTPS backend URL shown by Render. Do not infer it from the service name.

## 4. Create the Render frontend Static Site

1. Choose **New > Static Site**, select the same repository, and set Root Directory to `admin`.
2. Set Build Command to `npm ci && npm run build` and Publish Directory to `dist`.
3. Add the build-time variable `VITE_API_BASE_URL` with the actual backend HTTPS URL ending in `/api`, for example `https://<actual-backend-host>/api`. Do not include a placeholder or local IP.
4. Create the site and copy its actual HTTPS origin (scheme and host only).
5. Add that origin to the backend's `CORS_ALLOWED_ORIGINS`, retaining `https://localhost`, then redeploy the backend. Production rejects wildcard, HTTP, placeholders, and non-HTTPS CORS origins.

## 5. Verify database, health, and login

1. Check the Render backend logs for successful MySQL connection and Hibernate validation. A schema validation error means the production schema does not match the JPA entities; stop and reconcile it with a reviewed, backed-up migration. Do not change production to `ddl-auto=update` as a workaround.
2. Open `https://<actual-backend-host>/actuator/health`; expect HTTP 200 and an UP status.
3. Test login against the actual backend URL (PowerShell):

```powershell
$Backend = Read-Host "Actual HTTPS backend origin"
$Login = Invoke-RestMethod -Method Post -Uri "$Backend/api/auth/login" -ContentType "application/json" -Body '{"email":"admin@schooldemo.com","password":"Admin@123"}'
$Login.token
Invoke-RestMethod -Method Get -Uri "$Backend/api/auth/me" -Headers @{ Authorization = "Bearer $($Login.token)" }
```

The demo account is seeded only by the `development` profile. It is not created in production, so the demo credentials above will not work on Render. There is currently no production admin bootstrap endpoint. Before launch, use a controlled, audited DBA session after applying the schema baseline: generate a BCrypt hash locally with Spring Security's `BCryptPasswordEncoder`, then insert the first account into `accounts` with `email`, `password_hash`, `role='ADMIN'`, `name`, `phone` (nullable), and `active=1` (use the database's auto-generated `id`). Verify the account can sign in, then remove/revoke the bootstrap DBA access and rotate any temporary credentials. Never put the plaintext password or hash in source control or logs, and never reuse the demo password. The app has no server-side logout/revocation endpoint: clients log out by deleting their locally stored JWT; a token remains valid until expiry unless the signing secret is rotated.

## 6. Capacitor and Android APK

The existing Capacitor config uses `admin/dist`. Set `VITE_API_BASE_URL` to the real HTTPS API URL, then from the repository root run:

```powershell
npm --prefix admin ci
npm --prefix admin run build
npx cap sync android
Set-Location android
.\gradlew.bat assembleDebug
```

Debug APK output: `android/app/build/outputs/apk/debug/app-debug.apk`. Test login and an authenticated screen on a device using a different network, with the PC turned off. Android's WebView origin is allowed by the explicit `https://localhost` CORS entry. The release build command is `.\gradlew.bat assembleRelease`; release signing is not configured, so any output is unsigned and is not a production-signed APK. Configure a private signing key through secure CI/secrets before distribution. Do not commit the keystore.

The separate Expo application in `mobile/` is retained; it has its own `EXPO_PUBLIC_API_BASE_URL` configuration and is not what the Capacitor APK packages.

## 7. Uploads and persistence

The current backend declares `FILE_UPLOAD_PATH`, but inspection found no upload controller, multipart handling, or file-writing implementation, and no entity currently stores upload paths. No upload feature has been removed. If upload support is introduced later, Render container storage is ephemeral. Attach a paid persistent disk to the backend and point `FILE_UPLOAD_PATH` under its mount path, or integrate durable object storage. Back up uploaded data independently.

## Troubleshooting

- **Backend health check fails:** inspect startup logs for missing env vars, inaccessible MySQL, TLS/certificate errors, or JPA schema mismatch. Confirm the selected Render plan can reach the database and the health path is exactly `/actuator/health`.
- **Frontend build says API URL invalid:** set `VITE_API_BASE_URL` in the Static Site's build environment to the real HTTPS backend URL ending in `/api`, then redeploy.
- **Browser CORS error:** set `CORS_ALLOWED_ORIGINS` to the exact frontend origin (no path or trailing slash), comma-separated with `https://localhost`, and redeploy the backend.
- **Login returns 401:** production does not seed `admin@schooldemo.com`; provision an administrator account first. A 401 from `/api/auth/me` also means the bearer token is missing/expired/invalid.
- **Authenticated request returns 403:** the account is authenticated but lacks the required role/permission.
- **Android cannot connect:** rebuild after setting the real HTTPS API URL, run `npx cap sync android`, check device internet access and the backend health endpoint, and confirm the backend CORS list includes `https://localhost`.
- **Data appears missing after restart:** check whether the MySQL provider has persistent storage and whether the app connects to the intended database. Do not use local container storage for database files.

## Current deployment state

This repository is prepared for deployment but is not deployed or verified online by this task. No Render account, GitHub access, live MySQL credentials, service domains, production administrator, or signed Android keystore was supplied. Create the persistent MySQL service first and obtain its real connection details; then follow sections 3–6. Render's official guides: [MySQL](https://render.com/docs/deploy-mysql), [web services](https://render.com/docs/web-services), [static sites](https://render.com/docs/static-sites), [health checks](https://render.com/docs/health-checks), and [environment variables](https://render.com/docs/configure-environment-variables).
