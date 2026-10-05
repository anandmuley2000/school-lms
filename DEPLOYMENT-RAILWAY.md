# Railway deployment guide

## API service

Create a Railway project and a backend service from this repository. Set the service root to the repository root and use the included `Dockerfile`; it builds and runs the Spring Boot API. Provision MySQL 8 separately (Railway MySQL or an external provider) and make its TCP host reachable from the API service. Do not use `localhost` for a separately hosted database.

Set these variables on the API service:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_HOST` | MySQL hostname |
| `DB_PORT` | MySQL port, usually `3306` |
| `DB_NAME` | Database/schema name |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `DB_SSL_MODE` | `REQUIRED` when provider supports TLS |
| `JWT_SECRET` | Random secret of at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Comma separated exact HTTPS origins for deployed web clients |
| `DEMO_DATA_ENABLED` | `false` |

Railway provides `PORT`; the app binds to `0.0.0.0` and uses it automatically. Enable a public HTTPS domain for this service. Configure the health check as `/actuator/health`. The API base URL used by clients is `https://YOUR_API_HOST/api`.

## Database migration and initial admin

Take a database backup before any release. For an existing installation, run the numbered SQL migration in `backend/schema/migrations` against the selected schema, then deploy. `ddl-auto=validate` means startup rejects a schema that does not match the code. Demo accounts and reset endpoints are disabled in production. There is no production first-admin bootstrap endpoint today: an operator must provision the first ADMIN row through a one-time audited DBA operation, using a BCrypt password hash. The normal account-management endpoint becomes available after that first admin can sign in. Never seed production from the development fixture.

## Web admin

Build and host the existing Vite admin app as a static site. Set `VITE_API_BASE_URL=https://YOUR_API_HOST/api` at build time. Add the exact static-site HTTPS origin to `CORS_ALLOWED_ORIGINS`, then rebuild the admin site. The value must include scheme and host, without a trailing slash.

## Android client

The Expo app reads `EXPO_PUBLIC_API_BASE_URL` at build time (or `expo.extra.apiBaseUrl` from its app config). For a release, use `https://YOUR_API_HOST/api`, set `APP_ENV=production`, and build the Android app from `mobile/`. The app should not point at a workstation IP, localhost, or an HTTP endpoint. Android release signing is intentionally provided through private Gradle properties/CI secrets; the repository does not contain a production signing key. Keep the keystore and passwords outside source control and retain secure backups.

For a store-ready Expo release, provide `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` to the build process from a team-controlled keystore and CI secret store. The Gradle release config uses that key when all four values are present. Without it, a local `assembleRelease` falls back to the debug key for sideload testing only. Never upload that test APK to a store. If distributing the Capacitor wrapper instead, set `VITE_API_BASE_URL` before `npm run build` in `capacitor/`, sync the Android project, and sign its release APK/AAB with the same private-key handling.

## Release verification

After deploying, verify `/actuator/health`, sign in with a non-demo production account, and exercise the parent, teacher, and admin workflows against the production API. Test a second Android device on cellular data to confirm no local-network dependency. Treat this guide as configuration instructions; it does not indicate that a Railway service, domain, release keystore, or production account has already been created.
