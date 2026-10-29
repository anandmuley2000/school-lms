# Deploy the full School LMS to Render

This repository contains three parts: a Spring Boot API in `backend/`, a React/Vite portal in `admin/`, and an Expo mobile app in `mobile/`. The Render Blueprint at [`render.yaml`](render.yaml) deploys the API and portal as two services. The Android/Expo app remains a client; it is not a long-running Render service.

The API runs on Render and uses Aiven for MySQL. The frontend can remain on Netlify. The Render Blueprint defines the API and an optional Render static portal; do not create the portal service if Netlify is already serving the frontend.

## Render services

| Service | Runtime | Source/build settings | Health check |
| --- | --- | --- | --- |
| `school-lms-api` | Docker web service | Repository root; `Dockerfile` with repository root context | `/actuator/health` |
| `school-lms-portal` | Static site | Root directory `admin`; `npm ci && npm run build`; publish `dist` | Render serves the static site |

The backend Dockerfile copies `backend/` and the Maven wrapper from the repository root. Keep its service root directory at `/` and its Docker context at `.`. Render routes requests to the API on `PORT`; the service sets `PORT=8080` to match the image's exposed port, and Spring Boot binds to `0.0.0.0`.

## Deploy from the Blueprint

1. Push the repository to GitHub. In Render select **New > Blueprint**, connect `anandvmuley/school-lms`, and select the branch to deploy (normally `develop`). Render reads root-level `render.yaml` and defines the API plus a static portal. Since the live frontend is on Netlify, use only `school-lms-api` as the production API; do not switch the Netlify frontend to a duplicate Render portal. Review the plan and cost before confirming; the API is a paid `starter` web service in this Blueprint.
2. Create an Aiven for MySQL service in a region close to the Render API. Keep the source MySQL database running while you migrate. From the Aiven service's **Overview / Connection information**, record the hostname, port, database name, username, and password. Use a dedicated application user if available; keep its credentials private. Aiven requires TLS; this application can use `DB_SSL_MODE=REQUIRED` to encrypt traffic. Aiven requires its CA certificate when using `VERIFY_CA` or `VERIFY_IDENTITY`; configure the CA truststore before selecting either stricter mode. See [Aiven MySQL connection examples](https://aiven.io/docs/products/mysql/howto/list-code-samples) and [Aiven TLS requirements](https://aiven.io/docs/platform/concepts/tls-ssl-certificates).
3. Back up the current MySQL database before moving it. If Railway MySQL is the source, use its public TCP proxy endpoint from **Settings > Networking**; a `*.railway.internal` host is not reachable from outside Railway. Create a consistent dump using `mysqldump` with the source host, port, username, and database, then restore that dump into the Aiven database using the Aiven hostname, port, username, and database. Enter passwords only at the interactive prompt or through a protected local client option; do not put them in commands, Git, or shared logs. Keep the dump outside the repository and protect/delete it securely after validation. Do not stop or delete the source database until the Render API and data have been verified. Aiven's migration guide covers console-based migration options: [Migrate to Aiven MySQL](https://aiven.io/docs/products/mysql/howto/migrate-db-to-aiven-via-console).
4. For the selected fresh, empty Aiven database, set the Render API variables in the next step and deploy. Flyway now applies the checked-in migrations `V1` through `V4` automatically before Hibernate validates the JPA schema. V1 creates all existing core LMS tables; V2 adds timetable, leave-review and bus-route schema; V3 and V4 add the later event and reviewer fields. These migrations create schema only and do not seed application data. Do not manually run the baseline or point this deployment at a database with existing tables but no Flyway history: automatic baselining is disabled, so Flyway will stop safely and require an inspected migration plan for that database.
5. In Render's `school-lms-api` Environment settings, set `DB_HOST` to the Aiven hostname and `DB_PORT` to the Aiven port shown in Connection information (do not assume 3306). Set `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` to the Aiven database and user values, and set `DB_SSL_MODE=REQUIRED`. Never put database credentials in `render.yaml`, Git, or a frontend variable. Render environment values are configured in the dashboard or supplied through Blueprint placeholders, not committed as secrets; see [Render environment variables](https://render.com/docs/configure-environment-variables).
6. The Blueprint generates `JWT_SECRET` once and sets `SPRING_PROFILES_ACTIVE=production`, `DEMO_DATA_ENABLED=false`, and `PORT=8080`. Keep the generated JWT secret stable after deploys. Check API logs and `/actuator/health`; startup logs should show Flyway applying V1–V4 before Hibernate schema validation. Do not proceed if database access, migration, or schema validation fails.
7. Once the Netlify frontend has its production URL and Render has assigned the API URL, set the API's `CORS_ALLOWED_ORIGINS` to the exact Netlify origin (scheme and host only, no path or trailing slash), plus `https://localhost` for the Capacitor Android WebView. Save and redeploy the API. Set Netlify's build variable `VITE_API_URL` to `https://<actual-api-host>/api` using the actual Render API hostname, then rebuild the Netlify site.
8. For the first production administrator, use the one-time bootstrap below. Production intentionally does not seed demo accounts; do not try the development demo credentials.

### Create the first production administrator

The backend supports an explicitly enabled, production-profile bootstrap. In the Render API service's **Environment** settings, temporarily set:

| Variable | Value |
| --- | --- |
| `BOOTSTRAP_ADMIN_ENABLED` | `true` |
| `BOOTSTRAP_ADMIN_EMAIL` | The administrator's real email address |
| `BOOTSTRAP_ADMIN_PASSWORD` | A unique password of at least 12 characters; enter it only in Render's secret environment settings |
| `BOOTSTRAP_ADMIN_NAME` | The administrator's display name |

Save the environment and let Render redeploy. On startup, the backend creates the account only if the production database has no admin account. It stores a BCrypt hash, never the plain password. It will not overwrite an existing admin or promote an existing non-admin account; use a different email if the chosen address is already in use. Sign in at the portal, then immediately remove all four `BOOTSTRAP_ADMIN_*` variables from Render and redeploy. The setting defaults to disabled, is not an HTTP endpoint, and is ignored outside the production profile.

If the service already has an admin account but you do not know its password, this bootstrap intentionally does not reset it. Use a separately reviewed recovery procedure instead.

## Environment variables

### API (`school-lms-api`)

| Variable | Value |
| --- | --- |
| `PORT` | `8080` (Blueprint value; Render can inject/override the port) |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_HOST` | Aiven MySQL hostname from Connection information |
| `DB_PORT` | Aiven MySQL port from Connection information |
| `DB_NAME` | Restored LMS database name |
| `DB_USERNAME` | Aiven application DB user |
| `DB_PASSWORD` | Aiven application DB password |
| `DB_SSL_MODE` | `REQUIRED` for TLS encryption; use `VERIFY_CA`/`VERIFY_IDENTITY` only after installing Aiven's CA trust configuration |
| `JWT_SECRET` | Blueprint-generated secret (keep stable) |
| `CORS_ALLOWED_ORIGINS` | Exact portal origin and `https://localhost`, comma-separated |
| `DEMO_DATA_ENABLED` | `false` |

Do not set production `ddl-auto=update`; the production profile validates the schema after Flyway migrates it. `spring.flyway.baseline-on-migrate=false` prevents silently adopting an unknown existing schema, and Flyway `clean` is disabled. V1–V4 are packaged in the backend JAR at `db/migration` and run in version order on startup. Take and verify a database backup before applying future schema changes. If Aiven is not empty or has a `flyway_schema_history` table from a previous attempt, inspect its tables and history before proceeding; do not delete history or manually baseline it to bypass validation.

### Frontend (Netlify)

| Variable | Value |
| --- | --- |
| `VITE_API_URL` | Actual API URL ending in `/api`, e.g. `https://<actual-api-host>/api` |

Vite embeds this variable during the build. Changing it requires a new static-site build/deploy. Set it to the actual public Render API URL ending in `/api`; do not use localhost or a private database/API address.

## Existing mobile app

The Expo project in `mobile/` is not hosted as a Render web service. To build it for the Render API, set `EXPO_PUBLIC_API_BASE_URL` to the actual API URL ending in `/api` and use the app's production build configuration. The APK is still distributed separately. Do not change the mobile API URL to a placeholder or a local-network address for release builds.

## Troubleshooting

- **Blueprint cannot build the API:** confirm the API service uses root directory `/`, Dockerfile `./Dockerfile`, and Docker context `.`. The root Dockerfile needs `backend/`, `.mvn/`, and `mvnw` from the repository root.
- **API health check fails:** read startup logs. Common causes are a wrong Aiven host/port, incorrect credentials, TLS mode mismatch, or a restored schema that does not match the backend entities. Confirm the Aiven service is running and its connection details allow public client connections.
- **Flyway refuses startup because the schema is non-empty:** stop deployment and inspect the existing tables and `flyway_schema_history` first. Do not set `baseline-on-migrate=true`, drop tables, or rerun V1 manually. For an existing schema, prepare a reviewed baseline/migration plan that matches its actual schema and data.
- **Hibernate still reports a missing table after Flyway starts:** check logs for the successful applied migration versions and verify the Render `DB_NAME` points to the same Aiven schema used by Flyway. Do not disable Hibernate validation.
- **Aiven connection times out:** check the Aiven service status and IP filter. Aiven defaults to accepting public client IPs; if you restrict its IP filter, allow the Render API service's outbound IP ranges for its selected region and keep that list current. Render documents the ranges under the service's **Connect > Outbound** tab; see [Render outbound IP addresses](https://render.com/docs/outbound-ip-addresses) and [Aiven IP filtering](https://aiven.io/docs/platform/howto/restrict-access).
- **Portal build rejects API URL:** set `VITE_API_URL` in the static site's build environment to the actual public HTTPS API URL ending with `/api`, then redeploy.
- **Browser reports CORS:** set `CORS_ALLOWED_ORIGINS` to the Netlify site's exact origin and `https://localhost`, with comma separation, then redeploy the API.
- **Login fails:** production demo data is disabled. Provision a real administrator account through the documented secure database procedure.

## Local verification before first deploy

From the repository root, build the portal with a real HTTPS API URL in its build environment. Build the API with Maven or Docker. The Docker build requires the repository root as its context. No production credentials are needed in the repository.

This guide and Blueprint prepare the API for Render and Aiven; they do not create the Aiven service, migrate the live database, supply secrets, or perform an online deployment. Those steps require access to the Aiven and Render dashboards. Keep the source database until the cutover is verified.
