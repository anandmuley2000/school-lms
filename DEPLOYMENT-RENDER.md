# Deploy the full School LMS to Render

This repository contains three parts: a Spring Boot API in `backend/`, a React/Vite portal in `admin/`, and an Expo mobile app in `mobile/`. The Render Blueprint at [`render.yaml`](render.yaml) deploys the API and portal as two services. The Android/Expo app remains a client; it is not a long-running Render service.

The backend continues to use the existing MySQL database. This setup does not create, migrate, or replace the database. It also does not change the existing Railway deployment configuration.

## Render services

| Service | Runtime | Source/build settings | Health check |
| --- | --- | --- | --- |
| `school-lms-api` | Docker web service | Repository root; `Dockerfile` with repository root context | `/actuator/health` |
| `school-lms-portal` | Static site | Root directory `admin`; `npm ci && npm run build`; publish `dist` | Render serves the static site |

The backend Dockerfile copies `backend/` and the Maven wrapper from the repository root. Keep its service root directory at `/` and its Docker context at `.`. Render routes requests to the API on `PORT`; the service sets `PORT=8080` to match the image's exposed port, and Spring Boot binds to `0.0.0.0`.

## Deploy from the Blueprint

1. Push the repository to GitHub. In Render select **New > Blueprint**, connect `anandvmuley/school-lms`, and select the branch to deploy (normally `develop`). Render reads root-level `render.yaml` and creates the two services. Review the plan and cost before confirming; the API is a paid `starter` web service in this Blueprint because a school API should not be configured to sleep between requests.
2. During setup Render requests the API's database values, `CORS_ALLOWED_ORIGINS`, and the portal's `VITE_API_URL`. For the first API deployment, use the temporary CORS value `https://localhost` so the API can start. If Render requests `VITE_API_URL` before it has assigned the API hostname, enter a temporary valid HTTPS value such as `https://school-lms-api.onrender.com/api` so the static build can finish; the portal may not connect yet. After the API is live, copy its actual URL from Render, update `VITE_API_URL` to `<actual-api-url>/api`, and redeploy the portal. Do not leave the temporary value in production.
3. Copy database settings from the existing Railway MySQL service's public TCP connection details. The internal `*.railway.internal` host cannot be reached from Render. In Railway MySQL **Settings > Networking**, enable Public Access/TCP Proxy and copy the generated public hostname and proxy port. This exposes a database endpoint to the internet; use a dedicated least-privilege MySQL account and a strong password. Railway charges for traffic through its TCP proxy. See [Railway MySQL external connections](https://docs.railway.com/databases/mysql).
4. Confirm the existing database accepts connections through the proxy and that the app user has privileges from external hosts. Set Render's API `DB_HOST` and `DB_PORT` to the public TCP proxy host and port (not necessarily 3306). Set `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` to the real existing database values. Set `DB_SSL_MODE` to the mode supported by that database endpoint (`REQUIRED` only if the endpoint supports TLS; otherwise use the provider's documented setting). Never put database credentials in `render.yaml`, Git, or a frontend variable.
5. The Blueprint generates `JWT_SECRET` once and sets `SPRING_PROFILES_ACTIVE=production`, `DEMO_DATA_ENABLED=false`, and `PORT=8080`. Keep the generated JWT secret stable after deploys. Check API logs and `/actuator/health`; the app validates the existing schema and does not create or alter it. Do not proceed if database access or schema validation fails.
6. Once Render has assigned the portal's actual `onrender.com` URL, update the API's `CORS_ALLOWED_ORIGINS` to that exact origin (scheme and host only, no path or trailing slash), plus `https://localhost` for the Capacitor Android WebView. Save and redeploy the API. If Render assigned a different API hostname than the one used for the portal build, update the portal build variable `VITE_API_URL` to `https://<actual-api-host>/api` and redeploy the static site.
7. Verify the portal login and role-specific pages against the production API. Production intentionally does not seed demo accounts. Provision the first administrator using the project's controlled database process; do not try the development demo credentials.

## Environment variables

### API (`school-lms-api`)

| Variable | Value |
| --- | --- |
| `PORT` | `8080` (Blueprint value; Render can inject/override the port) |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_HOST` | Existing Railway MySQL public TCP proxy hostname |
| `DB_PORT` | Existing Railway MySQL public TCP proxy port |
| `DB_NAME` | Existing LMS database name |
| `DB_USERNAME` | Dedicated application DB user |
| `DB_PASSWORD` | Secret for that application DB user |
| `DB_SSL_MODE` | Value supported by the selected external endpoint |
| `JWT_SECRET` | Blueprint-generated secret (keep stable) |
| `CORS_ALLOWED_ORIGINS` | Exact portal origin and `https://localhost`, comma-separated |
| `DEMO_DATA_ENABLED` | `false` |

Do not set production `ddl-auto=update`; the production profile validates the schema. Take and verify a database backup before applying any schema changes. The backend currently uses reviewed SQL schema/migration files; run only the migration appropriate to the current database version.

### Portal (`school-lms-portal`)

| Variable | Value |
| --- | --- |
| `VITE_API_URL` | Actual API URL ending in `/api`, e.g. `https://<actual-api-host>/api` |

Vite embeds this variable during the build. Changing it requires a new static-site build/deploy. Do not use a Railway URL, localhost, or a private Railway hostname unless that is intentionally the active API.

## Existing mobile app

The Expo project in `mobile/` is not hosted as a Render web service. To build it for the Render API, set `EXPO_PUBLIC_API_BASE_URL` to the actual API URL ending in `/api` and use the app's production build configuration. The APK is still distributed separately. Do not change the mobile API URL to a placeholder or a local-network address for release builds.

## Troubleshooting

- **Blueprint cannot build the API:** confirm the API service uses root directory `/`, Dockerfile `./Dockerfile`, and Docker context `.`. The root Dockerfile needs `backend/`, `.mvn/`, and `mvnw` from the repository root.
- **API health check fails:** read startup logs. The usual causes are an unreachable Railway TCP proxy, incorrect credentials/privileges, unsupported TLS mode, or an existing database schema that does not match the backend entities.
- **Portal build rejects API URL:** set `VITE_API_URL` in the static site's build environment to the actual public HTTPS API URL ending with `/api`, then redeploy.
- **Browser reports CORS:** set `CORS_ALLOWED_ORIGINS` to the portal's exact origin and `https://localhost`, with comma separation, then redeploy the API.
- **Login fails:** production demo data is disabled. Provision a real administrator account through the documented secure database procedure.

## Local verification before first deploy

From the repository root, build the portal with a real HTTPS API URL in its build environment. Build the API with Maven or Docker. The Docker build requires the repository root as its context. No production credentials are needed in the repository.

This guide and Blueprint prepare the repository for Render; they do not create Render services, configure Railway's TCP proxy, supply secrets, or perform an online deployment. Those steps require access to the Render and Railway dashboards.
