# School LMS

Existing LMS application with a Java 21 / Spring Boot 3 API backed by MySQL, a React + Vite admin portal, an Expo React Native app, and a Capacitor Android wrapper for the admin portal. Clients authenticate with JWT bearer tokens and role-based access.

## Features

- Admin, teacher, and parent portals; parents can access every linked child from one account.
- Student records, attendance, class-and-division homework, results, fees, notifications, messages, events, class timetables, student-linked bus routes, and leave requests with review history.
- Admin account directory and role assignment. Student academic records link to parent accounts; student portals are disabled.
- Development-only demo data and reset, OpenAPI/Swagger, and Actuator health.
- Expo app with secure token storage and a separate Capacitor Android app.

## Architecture and role access

- **Backend:** Spring Boot REST API under `/api`, JWT authentication, BCrypt passwords, Spring Security role checks, and JPA repositories over MySQL 8.
- **Admin:** React/Vite portal for accounts, linked student records, attendance, academic data, fees, school events/holidays, messaging, leave, buses, and timetable management.
- **Teacher:** Assigned class/division scope controls student, attendance, homework, student leave, and timetable access. A teacher's own leave requests go to the admin review queue.
- **Parent:** One account can link to multiple student records. Parent API responses are limited to those children and their class/division resources.
- **Student records:** Students are not login users. Legacy `STUDENT` accounts are refused at login; adding a student only links the academic record to a parent. Deleting a student in the portal marks the record inactive to retain academic history.

## Operational workflows

- **Leave:** Parents request leave for a linked child. Teachers see/review student requests only for assigned class/divisions. Staff leave is unlinked and teacher requests can only be reviewed by admins. Decision notes and review timestamps are returned in leave history.
- **Homework and timetable:** Homework and class timetable entries are scoped by class plus division. Admins manage timetable CRUD; teachers and parents see only their permitted rows.
- **Buses:** Admins assign bus routes to student records. Parents see only routes assigned to their linked children. Location fields accept manual/simulated updates when GPS hardware is unavailable; the last-known coordinates and update time are exposed to authorized users.
- **Messaging:** Admins message teachers/parents; teachers and parents message school contacts only within their class/child relationship or admin. The API, rather than the UI alone, enforces these checks.
- **Calendar and reminders:** Events and holiday rows power the yearly calendar; scheduled reminders are deduplicated by target account/event. The mobile assistant answers from loaded data and routes users to app sections; it is a local navigation helper, not a generative remote chatbot.
- **Notifications:** Attendance, leave, messaging, and school updates use per-account notification records. Production demo seeding/reset is disabled.

Production schema is managed by Flyway migrations in `backend/schema/migrations/`. The application applies V1–V4 at startup before Hibernate validates the mappings. Automatic baselining and Flyway clean are disabled; review an existing database's schema and migration history before connecting it.

## Requirements

Java 21, MySQL 8 or compatible, Node.js 22+ and npm. Android builds additionally need the Android SDK/JDK setup expected by the Gradle wrapper.

## Local development

1. Create a local MySQL database/user. Copy `backend/.env.example` to a private local env file and set your database username/password and a unique `JWT_SECRET` of at least 32 UTF-8 bytes. Spring Boot does not load `.env` automatically, so export its values in your terminal or IDE environment. Never commit the populated file.
2. Start the backend in PowerShell from the repository root:

```powershell
$env:SPRING_PROFILES_ACTIVE = "development"
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

The development profile defaults only to MySQL at `localhost:3306` and database `school_lms`; provide `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in your environment. Backend URL: `http://localhost:8080`; health: `/actuator/health`; Swagger: `/swagger-ui/index.html`.

3. Start the React admin in a second terminal:

```powershell
Set-Location admin
Copy-Item .env.example .env
npm ci
npm run dev
```

The local API base is `http://localhost:8080/api`. For access from another device during development, set the API base to the development computer's LAN address and allow the browser origin in development CORS.

4. Start the Expo app in a third terminal if needed:

```powershell
Set-Location mobile
Copy-Item .env.example .env
npm ci
npx expo start
```

For a physical phone, set `EXPO_PUBLIC_API_BASE_URL` to an API address reachable from the phone. The phone's `localhost` is the phone itself.

## Accounts

Demo credentials are for local development only and are listed in the login screen. Demo accounts and demo data are seeded only under the `development` profile when explicitly enabled. Production does not create default accounts. Provision the first administrator securely before launch; never reuse a demo password.

When an admin adds a student, the record links to an existing parent account. Create parent and teacher accounts in the admin account directory.

## Checks

From the repository root:

```powershell
.\mvnw.cmd -f backend\pom.xml test
$env:VITE_API_URL = Read-Host "Set the deployed HTTPS API URL ending in /api"
npm --prefix admin run build
Set-Location mobile
npm ci
npx tsc --noEmit
```

Production frontend builds require the HTTPS backend API URL ending in `/api`; set `VITE_API_URL` to the active Render API URL in Netlify's build environment. For local frontend work use `admin/.env.example` and the development server. Production backend configuration uses MySQL with `ddl-auto=validate`; it does not create/update the schema. Capacitor's Android web assets are generated by `npx cap sync android` and are not committed; run sync after each build.

## Deployment

Follow [DEPLOYMENT-RENDER.md](DEPLOYMENT-RENDER.md) for the Render API and Aiven MySQL production configuration. The frontend can remain on Netlify. No deployment domain, database credentials, or release signing key is included in this repository.

Schema upgrades are versioned under `backend/schema/migrations`; back up production before deployment and add a forward migration for each schema change. Flyway executes packaged migrations before Hibernate's production `ddl-auto=validate` check.

To build a sideloadable Android APK, set `APP_ENV=production`, `EXPO_PUBLIC_APP_ENV=production`, and `EXPO_PUBLIC_API_BASE_URL=https://YOUR-RENDER-API-DOMAIN/api`, then run `mobile\android\gradlew.bat assembleRelease`. Provide the four `ANDROID_KEYSTORE_*`/alias variables documented in the release guide for team release signing. Without those variables, a local build uses the debug key and is for internal testing only.

## Team workflow

See [TEAM-WORKFLOW.md](TEAM-WORKFLOW.md) for branches, reviews, and teammate setup.
