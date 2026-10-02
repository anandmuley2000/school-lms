# Railway deployment guide

This guide prepares the existing Spring Boot API for Railway. It keeps the existing MySQL service and preserves the REST API.

## Repository configuration

- `Dockerfile`: multi-stage Java 21/Maven build and non-root Spring Boot runtime.
- `railway.json`: Dockerfile builder and `/actuator/health` health check.
- `application.yml`: common settings, bind address `0.0.0.0`, dynamic PORT with local fallback, development default profile.
- `application-development.yml`: local MySQL and development-only demo behavior.
- `application-production.yml`: Railway `SPRING_DATASOURCE_*`, JWT, CORS, and JPA settings.

Railway injects PORT and the application must listen on it for health checks and public HTTP traffic. See [Railway health checks](https://docs.railway.com/deployments/healthchecks) and [Railway config-as-code](https://docs.railway.com/config-as-code/reference).

## Local build

Run from the project root:

```powershell
.\mvnw.cmd -f backend\pom.xml clean verify
docker build -t school-lms-api .
```

Maven runs tests; the Docker build also runs `clean verify` before copying the executable JAR into the smaller runtime image. For local development, create `backend/.env` from `backend/.env.example`, load it into PowerShell, and use `SPRING_PROFILES_ACTIVE=development`. Local health URL: `http://localhost:8080/actuator/health`.

## Railway backend service

1. Connect the repository and set the backend service source to the repository root containing `Dockerfile` and `railway.json`.
2. Keep the existing Railway MySQL service and make its connection values available to the backend service.
3. Let Railway build from the Dockerfile; do not override the start command. The image runs `java -jar /app/app.jar`.
4. Set these backend variables:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `SPRING_DATASOURCE_URL` | Full JDBC URL `jdbc:mysql://host:port/database?...` using actual Railway MySQL values |
| `SPRING_DATASOURCE_USERNAME` | Actual Railway MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | Actual Railway MySQL password (secret) |
| `JWT_SECRET` | Unique random secret with at least 32 UTF-8 bytes (secret) |
| `CORS_ALLOWED_ORIGINS` | Exact HTTPS admin origin after it is published; comma-separated if needed |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Optional; defaults to `update` |
| `SPRING_JPA_SHOW_SQL` | Optional; defaults to `false` |
| `JWT_EXPIRATION_MS` | Optional; defaults to one day |
| `FILE_UPLOAD_PATH` | Optional; defaults to `./uploads` |

Do not set a fixed PORT; Railway injects it. The API falls back to 8080 only outside Railway. Supply the MySQL JDBC URL, username, and password through the standard SPRING_DATASOURCE variables. The URL is consumed as provided. Include TLS/timezone parameters if the database provider requires them. Do not use a `mysql://` value for the JDBC variable; use the `jdbc:mysql://` scheme.

Production `CORS_ALLOWED_ORIGINS` accepts only explicit HTTPS origins; wildcards are rejected. If unset, the backend starts with no browser origins allowed, so add the real admin origin before using the browser admin. Do not use `*`.

5. Deploy the backend. Railway builds the Docker image and checks `/actuator/health` before marking it healthy.
6. Generate a public domain for the healthy backend service. Railway supplies the real URL; this repository does not invent one.

## Admin production build

After receiving the actual HTTPS backend URL, build the Vite app with `VITE_API_BASE_URL` ending in `/api`. Production config rejects missing, placeholder, and non-HTTPS URLs.

```powershell
Set-Location admin
$ApiUrl = Read-Host "Enter the real HTTPS API base URL ending in /api"
if ($ApiUrl -notmatch '^https://.+/api/?$') { throw "Enter the real HTTPS API URL ending in /api." }
Set-Content .env.production "VITE_API_BASE_URL=$ApiUrl"
npm ci
npm run build
```

Publish `admin/dist` with a static host, then set that host's exact HTTPS origin in Railway CORS.

## Mobile production build

Set EAS production variable `EXPO_PUBLIC_API_BASE_URL` to the real backend HTTPS URL ending in `/api`, then build with the existing production Android APK profile. Wait until the backend domain and health check are live before building a distributable APK.

## Database, uploads, and safety

- Production uses the existing Railway MySQL. Hibernate defaults to `update`, which adds compatible schema changes without dropping tables; back up and review schema changes before rollout. `create` and `create-drop` are not used.
- Production demo seeding/reset and SQL initialization are disabled.
- The current source defines `FILE_UPLOAD_PATH` but has no upload controller or storage integration. If uploads are added later, Railway container files are ephemeral without a persistent volume; attach one or configure object storage.
- Never put real secrets in example files, Docker layers, frontend EXPO_PUBLIC variables, Git, or logs.

## Remaining manual steps

- Set actual MySQL/JWT/CORS variables in Railway.
- Verify the existing database and take a backup before deployment.
- Deploy the branch with these changes and generate the actual backend domain.
- Build and publish the admin with the real API URL, then set its origin in CORS.
- Verify `https://<actual-backend-domain>/actuator/health` returns HTTP 200.

No live Railway deployment or production database change is performed by this guide.
