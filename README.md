# Northstar School LMS

See [DEPLOYMENT.md](DEPLOYMENT.md) for production environment and build commands. See [TEAM-WORKFLOW.md](TEAM-WORKFLOW.md) for Git branch, GitHub, and teammate instructions.

Existing school LMS: Spring Boot 3 / Java 21 / MySQL API, React + Vite admin portal, and Expo React Native app. The clients call the API over REST with JWT bearer authentication. Roles are ADMIN, TEACHER, PARENT, and STUDENT.

## Features

- Role-aware authentication with BCrypt password hashes and JWT authorization.
- Students, attendance, homework, notifications, exam results, fees, messaging, school events, and leave requests.
- Admin event management and leave review; parents/students submit leave requests for linked students; teachers may submit staff leave requests.
- Development-only demo seeding and destructive demo reset. Both are excluded from production.
- OpenAPI/Swagger UI and Actuator health endpoint.
- Mobile access token persistence with Expo SecureStore, request timeouts, and readable API/network errors.
- Development and production configuration for API URL, CORS, MySQL TLS, JWT secret, and schema validation.

## Requirements

- Java 21, Maven 3.9+, MySQL 8+, Node.js 22+ and npm.
- Android Studio / Android SDK for local Android APK builds, or EAS CLI and an Expo account for cloud builds.
- A deployed HTTPS API domain is required for a distributable production APK and production admin bundle. No domain or hosting provider is configured in this project.

## Backend local development

1. Create a local MySQL database/user. Copy `backend/.env.example` to `backend/.env` and set the local MySQL password and a random local `JWT_SECRET` of at least 32 characters. Keep `.env` files private; Spring does not load them automatically.
2. In PowerShell, from the project root, import local values and start Spring Boot:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
Get-Content backend\.env | ForEach-Object { if ($_ -match '^([^=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') } }
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

The default profile is `development`; Hibernate updates the local schema and demo records are seeded. Swagger UI: `http://localhost:8080/swagger-ui/index.html`. Health: `http://localhost:8080/actuator/health`.

## Admin local development

```powershell
cd admin
npm install
Copy-Item .env.example .env
npm run dev
```

Open `http://localhost:5173`. Set `VITE_API_BASE_URL` in the local env file if the API is not at `http://localhost:8080/api`.

## Mobile local development

```powershell
cd mobile
npm install
npx expo start
```

For a physical device, set `EXPO_PUBLIC_API_BASE_URL` to the computer's reachable LAN URL; the phone's `localhost` refers to itself. For a production APK, set `APP_ENV=production`, `EXPO_PUBLIC_APP_ENV=production`, and `EXPO_PUBLIC_API_BASE_URL` to your real deployed HTTPS API base ending in `/api`. The production config intentionally rejects missing, placeholder, or non-HTTPS API URLs.

## Production deployment configuration

Use `backend/.env.production.example`, `admin/.env.production.example`, `mobile/.env.production.example`, and the root `.env.example` as templates. Configure the real host environment with:

- `SPRING_PROFILES_ACTIVE=production`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` for a provisioned MySQL 8 service with TLS enabled.
- `JWT_SECRET` as a unique, random secret of at least 32 bytes. Do not reuse demo credentials or commit this value.
- `CORS_ALLOWED_ORIGINS` as the exact HTTPS admin origin(s), comma-separated; wildcards and non-HTTPS origins are rejected in production. Native Android/iOS apps do not send browser origins, so they do not need a CORS entry.
- `VITE_API_BASE_URL` and `EXPO_PUBLIC_API_BASE_URL` as the deployed HTTPS API base URL ending in `/api`.

The production profile uses `ddl-auto=validate`, requires an existing compatible schema, disables SQL init/demo reset, and does not seed demo accounts or students. Before first launch, provision the production schema from reviewed/versioned DDL and migrate it; never copy local demo rows into production. A migration tool/schema baseline is still an operational deployment task. The app does not deploy itself or create a database account.

## Builds

Run backend tests and produce the executable JAR:

```powershell
cd "D:\student app"
.\mvnw.cmd -f backend\pom.xml clean package
```

Build the admin for development checks with `npm run build -- --mode development`; a production build uses `npm run build` after setting a real HTTPS `VITE_API_BASE_URL` in the production environment. Type-check mobile with `npx tsc --noEmit` from `mobile`.

For a production admin build on Windows, enter the real API base URL and build:

```powershell
cd admin
$ApiUrl = Read-Host 'Enter the deployed HTTPS API base URL ending in /api'
Set-Content .env.production "VITE_API_BASE_URL=$ApiUrl"
npm run build
```

Build an installable production APK via EAS. This needs an Expo account and a project linked with EAS; it builds an artifact but does not publish or deploy the app. See `DEPLOYMENT.md` for exact configuration commands; no API URL is invented here.

The production profile in `mobile/eas.json` selects the production EAS environment, marks the app as production, and sets Android output to APK. The Expo documentation confirms `android.buildType: apk` selects APK output and EAS environment variables are required on the remote build worker. Do not distribute a development APK that points to localhost or an untrusted HTTP address.

## Demo accounts

Demo account shortcuts are shown only in development builds; the backend seeds those users only in the development profile. The `POST /api/admin/demo/reset` route exists only in development and is restricted to admins. Never expose a development profile to the public internet.

## Project layout

- `backend/`: Spring Boot REST API, JPA entities, authorization, profiles, and database config.
- `admin/`: React + TypeScript + Vite school admin portal.
- `mobile/`: Expo + React Native app, secure token storage, and EAS APK profiles.

See the individual `.env.example` files for variable names. Real `.env` files are ignored by Git; never paste secrets into source files or commit them.

See [DEPLOYMENT.md](DEPLOYMENT.md) for production configuration and exact build commands, and [TEAM-WORKFLOW.md](TEAM-WORKFLOW.md) for the local Git branch workflow and teammate setup.
