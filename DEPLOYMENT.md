# Deployment

The supported production target for the current project is Railway with an external MySQL 8 database. Follow [DEPLOYMENT-RAILWAY.md](DEPLOYMENT-RAILWAY.md) for the service setup and exact environment variables. Production startup validates the schema and does not seed demo data.

Apply the numbered SQL files in `backend/schema/migrations` in order to a verified database backup before deploying schema changes. The SQL files are packaged in the API JAR but are intentionally applied manually; the API uses Hibernate `ddl-auto=validate` and fails fast if a migration is missing.

For a mobile release, build from `mobile/` with `APP_ENV=production`, `EXPO_PUBLIC_API_BASE_URL=https://YOUR_API_HOST/api`, and a production Android signing key supplied through local Gradle properties or CI secrets. Never commit signing keys or database/JWT credentials.
