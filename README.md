# School LMS

Existing school LMS: Spring Boot 3 / Java 21 / MySQL backend, React + Vite admin portal, and Expo React Native mobile app. The clients use JWT bearer authentication and role-based access.

## Features

- Admin, teacher, parent, and student dashboards.
- Student records, attendance, homework, results, fees, notifications, messages, events, and leave requests.
- Admin account directory and role assignment. Creating a student also creates and links a STUDENT login account.
- Development-only demo seeding/reset, OpenAPI/Swagger, and Actuator health.
- Expo mobile app with secure token storage.

## Requirements

Java 21, MySQL 8 or compatible, Node.js 22+ and npm. Docker is needed for a local production-image build; Railway is needed for hosting.

## Local development

Copy `backend/.env.example` to `backend/.env`. Set a local MySQL user/password and a unique local `JWT_SECRET` of at least 32 UTF-8 bytes. Keep real .env files private; Spring Boot does not load them automatically.

From PowerShell opened at the project root:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
Get-Content backend\.env | ForEach-Object { if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)\s*$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"').Trim("'"), 'Process') } }
$env:SPRING_PROFILES_ACTIVE = "development"
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Backend: `http://localhost:8080`; health: `/actuator/health`; Swagger: `/swagger-ui/index.html`.

Start the admin in another terminal:

```powershell
Set-Location admin
Copy-Item .env.example .env
npm ci
npm run dev
```

Local admin API config is `VITE_API_BASE_URL=http://localhost:8080/api`. From a different device on the same Wi-Fi, use the computer's LAN address for the API and allow the admin page origin in development CORS.

Start mobile in a third terminal:

```powershell
Set-Location mobile
Copy-Item .env.example .env
npm ci
npx expo start
```

On a physical phone, set `EXPO_PUBLIC_API_BASE_URL` to the computer's reachable LAN API URL. The phone's localhost refers to itself.

## Student and staff login

When an admin adds a student, the form requires a student login email and initial password. The backend creates a STUDENT account, hashes the password with BCrypt, and links it to the student record in one transaction. Give the student those credentials securely; the same sign-in page opens the Student dashboard. The API never returns the password.

Create parent and teacher accounts in the admin account directory, assign their role, and use the parent's account email when linking a student. Demo shortcuts are development-only; production does not seed demo accounts.

## Checks and builds

Backend tests and executable JAR:

```powershell
.\mvnw.cmd -f backend\pom.xml clean verify
```

Admin development build:

```powershell
Set-Location admin
npm ci
npm run build -- --mode development
```

Mobile type check:

```powershell
Set-Location mobile
npm ci
npx tsc --noEmit
```

Local production container build:

```powershell
docker build -t school-lms-api .
```

## Railway backend

The root `Dockerfile` builds and tests the existing Maven backend with Java 21. `railway.json` selects that Dockerfile and checks `/actuator/health`. Connect the repository root as the Railway backend service and keep the existing MySQL service.

Set these variables in Railway, using the actual values from your MySQL service. Never commit or print secret values.

| Variable | Setting |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `SPRING_DATASOURCE_URL` | Full JDBC URL beginning `jdbc:mysql://`, using the real host, port, and database. Keep any provider-required TLS/timezone parameters. |
| `SPRING_DATASOURCE_USERNAME` | Railway MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | Railway MySQL password; store as a secret |
| `JWT_SECRET` | Unique random value of at least 32 UTF-8 bytes; store as a secret |
| `CORS_ALLOWED_ORIGINS` | Exact HTTPS admin origin, without a path or trailing slash; comma-separate multiple origins. If unset, the backend starts with no browser origins allowed. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Optional; defaults to `update`; review schema changes and back up data |
| `SPRING_JPA_SHOW_SQL` | Optional; defaults to `false` |
| `JWT_EXPIRATION_MS` | Optional; defaults to `86400000` |
| `FILE_UPLOAD_PATH` | Optional; defaults to `./uploads` |
| `PORT` | Injected dynamically by Railway; do not set a fixed production port |

The backend binds to `0.0.0.0` and listens on Railway's injected PORT (fallback 8080 outside Railway). Database URL/username/password and JWT secret are required. Production rejects wildcard and non-HTTPS CORS origins. If the CORS variable is empty, the service starts but browser requests are denied until the real admin origin is added. Do not use `*`.

Railway uses the root Dockerfile and its default start command, `java -jar /app/app.jar`. Generate a real backend domain after deployment; no production URL is hard-coded.

### Build and publish the admin

Vite embeds `VITE_API_BASE_URL` at build time. After Railway provides the real HTTPS backend URL:

```powershell
Set-Location admin
$ApiUrl = Read-Host "Enter the real HTTPS API base ending in /api"
if ($ApiUrl -notmatch '^https://.+/api/?$') { throw "Enter the real HTTPS API URL ending in /api." }
Set-Content .env.production "VITE_API_BASE_URL=$ApiUrl"
npm ci
npm run build
```

Publish `admin/dist` with your static host, then set that host's exact HTTPS origin as `CORS_ALLOWED_ORIGINS` in Railway.

### Production mobile API and APK

Set EAS production variable `EXPO_PUBLIC_API_BASE_URL` to the real Railway HTTPS API base ending in `/api`, then use the existing production EAS APK profile. Production config rejects missing, placeholder, and non-HTTPS URLs. Rebuild when the embedded URL or native config changes.

## Operations and team

- The current source defines `FILE_UPLOAD_PATH` but has no upload controller or storage integration. If uploads are added later, Railway's container filesystem is ephemeral without a persistent volume; attach one or configure object storage.
- Production disables demo seeding/reset and SQL initialization. JPA defaults to `update` to preserve data; review schema changes and backups before deployment. Never use `create` or `create-drop` in production.
- This repository is prepared for deployment; these instructions do not deploy services, change production database data, publish the admin, or distribute an APK.
- Never commit real .env files, database dumps, Railway credentials, JWT secrets, signing keys, or generated APKs.
- See [TEAM-WORKFLOW.md](TEAM-WORKFLOW.md) for branches, reviews, and teammate setup.
