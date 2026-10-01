# Deployment preparation

This guide prepares the existing LMS for hosting. It does not deploy the backend or admin site, create a production database, or build an APK. No provider, production domain, or production credentials have been selected.

## Current architecture

- `backend/`: Spring Boot 3.5, Java 21, MySQL, JPA, Spring Security, JWT.
- `admin/`: React + Vite static web app.
- `mobile/`: Expo / React Native Android and iOS app.
- The admin and mobile clients share the REST API and send JWT bearer tokens.

## Local development

### Backend

Use the development profile (the default). Copy `backend/.env.example` to `backend/.env` and enter local MySQL settings. Import those values into the current PowerShell session; Spring does not load `.env` files itself:

```powershell
cd "D:\student app"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
Get-Content backend\.env | ForEach-Object { if ($_ -match '^([^=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') } }
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

The local API is `http://localhost:8080/api`; Swagger UI is `http://localhost:8080/swagger-ui/index.html`. The development profile uses MySQL on localhost, local schema update, development-only demo seeding, and the admin-only demo reset route. Supply a local `JWT_SECRET` through `backend/.env`; there is no fallback secret in source configuration.

### Admin

```powershell
cd "D:\student app\admin"
npm ci
npm run dev
```

The ignored local `admin/.env` sets `VITE_API_BASE_URL=http://localhost:8080/api`.

### Mobile

```powershell
cd "D:\student app\mobile"
npm ci
npx expo start
```

The ignored local `mobile/.env` sets `APP_ENV=development`, `EXPO_PUBLIC_APP_ENV=development`, and `EXPO_PUBLIC_API_BASE_URL=http://localhost:8080/api`. On a physical phone, use the development computer's reachable LAN address instead of localhost.

## Production environment variables

Set these in the backend host's secret/environment-variable manager. Do not commit them or copy a local `.env` to the host.

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Must be `production`. |
| `DB_HOST` | Production MySQL hostname; its TLS certificate must match this name. |
| `DB_PORT` | MySQL port; defaults to `3306`. |
| `DB_NAME` | Existing production schema/database name. |
| `DB_USERNAME` | Production database user with only the privileges the app needs. |
| `DB_PASSWORD` | Production database password, stored as a host secret. |
| `JWT_SECRET` | Unique random secret of at least 32 bytes, stored as a host secret. Rotating it invalidates existing tokens. |
| `CORS_ALLOWED_ORIGINS` | Exact HTTPS admin site origin(s), comma-separated, with no path or trailing slash. Wildcards and HTTP origins are rejected in production. |
| `PORT` | Optional host-provided HTTP port; defaults to `8080`. |
| `JWT_EXPIRATION_MS` | Optional JWT lifetime; defaults to one day. |
| `FILE_UPLOAD_PATH` | Optional writable persistent path if uploads are enabled by the application. |

The mobile app is a native client and does not need a browser CORS origin. If a browser-based mobile/web client is added, add its exact HTTPS origin too.

The production profile requires MySQL and JDBC `sslMode=VERIFY_IDENTITY`; it requires a provisioned schema and uses Hibernate `validate`. It does not create the schema, alter its structure, seed demo accounts, or expose the reset controller. There is no migration framework in the current project, so provision/update schema from reviewed DDL before starting the production API. Do not run production with the `development` profile or `ddl-auto=update`.

## Backend build and run

From the project root, build the existing backend with Java 21 and the checked-in wrapper:

```powershell
cd "D:\student app"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw.cmd -f backend\pom.xml clean package
```

Expected artifact: `backend\target\school-lms-api-0.1.0.jar`.

After the deployment host has the variables above, its start command is:

```powershell
java -jar backend\target\school-lms-api-0.1.0.jar
```

Use the provider's environment/secret manager and process manager to set variables and keep the service running. The health endpoint is `/actuator/health`. No provider-specific publish command can be supplied until a host is chosen.

## Admin production build

The production API URL is provided only during build. It must be the real HTTPS base ending in `/api`; the Vite config stops the build if it is missing, HTTP, or still a placeholder.

```powershell
cd "D:\student app\admin"
$ApiUrl = Read-Host "Enter the deployed HTTPS API base URL ending in /api"
if ($ApiUrl -notmatch '^https://.+/api/?$' -or $ApiUrl -match 'YOUR-ONLINE-API-DOMAIN') { throw "Use the real HTTPS API URL ending in /api." }
Set-Content .env.production "VITE_API_BASE_URL=$ApiUrl"
npm ci
npm run build
```

Build output is in `admin\dist`. Upload that directory to the selected static hosting provider only after reviewing the build and choosing the provider. No deployment is performed here.

## Production mobile API and APK

The production EAS profile sets `APP_ENV=production` and `EXPO_PUBLIC_APP_ENV=production`. Store the actual API base URL in the EAS `production` environment; it is a public client setting, not a secret. The app configuration rejects a missing, HTTP, or placeholder URL. EAS creates an APK using the existing `android.buildType=apk` profile.

```powershell
cd "D:\student app\mobile"
npx eas-cli@latest login
npx eas-cli@latest init
$ApiUrl = Read-Host "Enter the deployed HTTPS API base URL ending in /api"
if ($ApiUrl -notmatch '^https://.+/api/?$' -or $ApiUrl -match 'YOUR-ONLINE-API-DOMAIN') { throw "Use the real HTTPS API URL ending in /api." }
npx eas-cli@latest env:set --name EXPO_PUBLIC_API_BASE_URL --value $ApiUrl --environment production --visibility plaintext
npx eas-cli@latest build --platform android --profile production
```

This requests a cloud build artifact; it does not publish the app to an app store. It requires an Expo account and an EAS-linked project. Do not run the production build until the API is deployed and reachable by HTTPS.

## Backend updates and APK updates

- Deploying a backend fix that preserves the existing API contract does not require rebuilding the admin bundle or mobile APK.
- Admin code changes require a new admin build and static-site update.
- Mobile UI/client code changes require a new APK with the current configuration. The project does not currently configure Expo Updates for production JavaScript updates.
- Changing `EXPO_PUBLIC_API_BASE_URL`, Android permissions, native dependencies, app identifiers, or other embedded native configuration requires a new APK.
- A breaking API change may require coordinated admin/mobile updates. Keep API contracts compatible during staged backend releases.

## Credentials and local files

The repository ignores `.env`, `.env.*` other than example templates, keystores, local Android config, build outputs, logs, and IDE files. Local `.env` values remain on this machine. Production credentials belong in the host's secret manager and must never be put in frontend `EXPO_PUBLIC_*` variables.
