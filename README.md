# School Management System

A full-stack School Management System: a Spring Boot 3 / Java 21 REST API
backend, a React 19 + TypeScript (Vite, MUI) frontend, and a MySQL 8
database — covering student/teacher/staff records, attendance, fees,
exams, library, transport, hostel, payroll, assignments, online classes,
notices/calendar, admissions, and reporting/analytics.

The full data model (tables, columns, enums, seed conventions) lives in
[`SCHEMA_CONTRACT.md`](./SCHEMA_CONTRACT.md) — treat it as the single
source of truth alongside this README.

## Table of contents

1. [Tech stack](#tech-stack)
2. [Features](#features)
3. [Screenshots](#screenshots)
4. [Project structure](#project-structure)
5. [Prerequisites](#prerequisites)
6. [Installation](#installation)
7. [Environment variables](#environment-variables)
8. [Demo login credentials](#demo-login-credentials)
9. [API documentation](#api-documentation)
10. [Docker / deployment](#docker--deployment)
11. [Architecture](#architecture)

## Tech stack

| Layer | Technology | Version (from repo) |
|---|---|---|
| Backend language/runtime | Java | 21 |
| Backend framework | Spring Boot (`spring-boot-starter-parent`) | 3.3.4 |
| Backend build tool | Maven (via the included `mvnw` wrapper) | wrapper targets Maven 3.9.9 |
| Backend web layer | Spring Web (MVC), Spring Security, Spring Data JPA | via Spring Boot starters |
| Auth | JWT (`io.jsonwebtoken` / jjwt) | 0.12.6 |
| API docs | springdoc-openapi (Swagger UI) | 2.6.0 |
| Object mapping | MapStruct | 1.5.5.Final |
| Boilerplate reduction | Lombok | (Spring Boot–managed version) |
| Database driver | MySQL Connector/J | (Spring Boot–managed version) |
| Database | MySQL | 8.0 (window functions in seed scripts require 8.0+) |
| Frontend language | TypeScript | 5.6.3 |
| Frontend framework | React | 19.0.0 |
| Frontend build tool | Vite | 5.4.11 |
| Frontend UI kit | MUI (`@mui/material`, `@mui/x-data-grid`, `@mui/x-date-pickers`) | 6.1.6 / 7.23.0 |
| Frontend state | Redux Toolkit + React Redux | 2.12.0 / 9.2.0 |
| Frontend routing | React Router DOM | 6.28.0 |
| Frontend forms/validation | React Hook Form + Yup | 7.53.2 / 1.4.0 |
| Frontend charts | Recharts | 2.15.4 |
| Frontend HTTP client | Axios | 1.7.7 |

Maven coordinates: groupId `com.school.sms`, artifactId `school-backend`,
package root `com.school.sms`. Frontend package name `school-frontend`.

## Features

Reflects what is actually implemented (backend controllers + frontend
pages/API modules), organized by module:

- **Auth & accounts** — JWT login/refresh/logout, password reset flow,
  11 fixed roles (`SUPER_ADMIN`, `PRINCIPAL`, `VICE_PRINCIPAL`, `TEACHER`,
  `CLASS_TEACHER`, `ACCOUNTANT`, `LIBRARIAN`, `RECEPTIONIST`, `STUDENT`,
  `PARENT`, `SECURITY_GUARD`) with role/permission-based access control.
- **Academic setup** — academic years, departments, designations, classes,
  sections, subjects, and class/section/subject/teacher assignment.
- **Students** — admissions, profiles, guardians, medical details,
  documents, status (active/inactive/alumni/transferred).
- **Teachers & staff** — profiles, employment details, department/
  designation assignment.
- **Attendance** — daily student attendance and teacher attendance, plus
  leave applications (teacher/staff/student) with an approval workflow.
- **Fees** — fee categories, fee structures per class/year, per-student
  fee tracking, fee payments/receipts, scholarships, a fee dashboard.
- **Exams** — exam types, exams, exam schedules, grades, marks entry.
- **Assignments & online classes** — assignment creation/submission/
  grading, scheduled online class links.
- **Library** — book catalog/categories, issue/return tracking with
  overdue fines, a library dashboard.
- **Transport** — drivers, buses, routes, pickup points, per-student
  transport assignment.
- **Hostel** — hostels, rooms, student allocation, visitor logs, hostel
  fees.
- **Payroll** — salary structures and monthly payroll runs for teachers
  and staff.
- **Communication** — notices (role-targeted), a school calendar/events
  feed, notifications.
- **Admissions** — a public admission-enquiry endpoint plus an internal
  admin view for processing enquiries.
- **Reports & analytics** — a dedicated reports/analytics API and
  frontend reports/dashboard pages backed by SQL views
  (`vw_student_summary`, `vw_fee_collection_summary`,
  `vw_attendance_summary`, `vw_teacher_workload`, `vw_library_overdue`,
  `vw_class_section_strength`, `vw_student_fee_due`).
- **Settings** — school info and system settings management.

The frontend has a page/API module for every one of the areas above (see
[Project structure](#project-structure)), including a role-scoped parent
portal (`src/pages/parent`).

## Screenshots

Placeholders only — replace these with real screenshots of the running
app (`docs/screenshots/.gitkeep` marks the folder so it survives in git
until you add images):

| | |
|---|---|
| Login | ![Login](docs/screenshots/login.png) |
| Dashboard | ![Dashboard](docs/screenshots/dashboard.png) |
| Students | ![Students](docs/screenshots/students.png) |
| Fees | ![Fees](docs/screenshots/fees.png) |
| Attendance | ![Attendance](docs/screenshots/attendance.png) |
| Reports | ![Reports](docs/screenshots/reports.png) |

## Project structure

```
school/
├── SCHEMA_CONTRACT.md          # Single source of truth for the data model
├── README.md                   # This file
├── docker-compose.yml          # mysql + backend + frontend, one command up
├── .env.example                # Compose-level secrets/config template
├── nginx/
│   └── nginx.conf              # Reference single-domain reverse-proxy config
├── docs/
│   └── screenshots/            # Placeholder images for this README
├── database/                   # MySQL 8 scripts, run 00 -> 07 in order
│   ├── 00_create_database.sql
│   ├── 01_schema.sql
│   ├── 02_indexes.sql
│   ├── 03_views.sql
│   ├── 04_triggers.sql
│   ├── 05_procedures.sql
│   ├── 06_seed_reference_data.sql
│   ├── 07_sample_data.sql
│   └── README.md               # Database-specific docs (row counts, etc.)
├── school-backend/              # Spring Boot API (Java 21, Maven)
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── mvnw / mvnw.cmd / .mvn/
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/java/com/school/sms/
│       │   ├── SchoolManagementApplication.java
│       │   ├── config/          # Security, CORS, Jackson, OpenAPI, async, JPA auditing, MVC
│       │   ├── controller/      # ~54 REST controllers, one per resource/module
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   └── response/
│       │   ├── entity/          # 79 JPA entities (1:1 with SCHEMA_CONTRACT.md tables)
│       │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│       │   ├── mapper/          # MapStruct entity <-> DTO mappers
│       │   ├── repository/      # Spring Data JPA repositories
│       │   ├── security/        # JWT filter/provider, UserDetailsService, guards
│       │   ├── service/         # Interfaces + service/impl/ implementations
│       │   └── util/            # Constants, date/name/grade helpers, JPA Specifications
│       ├── main/resources/
│       │   ├── application.yml       # Shared config, all env-var overrides
│       │   ├── application-dev.yml   # Dev profile (SQL logging, Swagger on)
│       │   ├── application-prod.yml  # Prod profile (Swagger off, quieter logs)
│       │   └── logback-spring.xml
│       └── test/java/com/school/    # Test sources
└── school-frontend/             # React 19 + TypeScript SPA (Vite)
    ├── Dockerfile
    ├── .dockerignore
    ├── nginx.conf                # SPA-fallback config baked into the frontend image
    ├── package.json
    ├── vite.config.ts
    ├── .env.example / .env.development
    ├── README.md
    └── src/
        ├── main.tsx / App.tsx (entry point)
        ├── api/                  # One axios-based API module per backend resource
        ├── components/
        │   ├── charts/           # Recharts wrapper cards (bar/line/pie/area)
        │   └── common/           # DataTable, PageHeader, StatCard, dialogs, etc.
        ├── hooks/
        ├── layouts/              # DashboardLayout, AuthLayout, PublicLayout, Sidebar, TopBar
        ├── pages/                # One folder per module: students, teachers, attendance,
        │                         # fees, exams, library, transport, hostel, payroll,
        │                         # assignments, online-classes, notices, calendar,
        │                         # communication, admission, reports, settings, parent, auth...
        ├── routes/               # AppRouter, ProtectedRoute, RoleBasedRoute
        ├── store/                # Redux Toolkit slices (auth, notifications, ui)
        ├── theme/                # MUI theme + dark/light mode provider
        ├── types/
        └── utils/                # CSV export, blob download, formatting helpers
```

## Prerequisites

- **Java 21** (JDK) — required to run the backend directly on the host.
  Maven itself is *not* required to be installed separately: the repo
  ships the Maven Wrapper (`mvnw` / `mvnw.cmd`), which downloads and uses
  the correct Maven version (3.9.9) automatically.
- **Node.js 20+** and npm — for the frontend (`node:20-alpine` is the
  Docker build image; match that locally too).
- **MySQL 8.0+** — the seed scripts use `ROW_NUMBER() OVER (...)` window
  functions that require MySQL 8.
- **Docker & Docker Compose** — optional, only needed for the
  containerized setup described in [Docker / deployment](#docker--deployment).

## Installation

### 1. Clone

```bash
git clone <this-repo-url>
cd school
```

### 2. Database setup

**Option A — manual MySQL scripts** (run in this exact numeric order
against a MySQL 8 server; each script issues its own `USE
school_management_system;`):

```bash
cd database
mysql -u root -p                          < 00_create_database.sql
mysql -u root -p school_management_system  < 01_schema.sql
mysql -u root -p school_management_system  < 02_indexes.sql
mysql -u root -p school_management_system  < 03_views.sql
mysql -u root -p school_management_system  < 04_triggers.sql
mysql -u root -p school_management_system  < 05_procedures.sql
mysql -u root -p school_management_system  < 06_seed_reference_data.sql
mysql -u root -p school_management_system  < 07_sample_data.sql
```

(`07_sample_data.sql` loops hundreds of times generating sample records —
expect it to take anywhere from a few seconds to a couple of minutes.)
See [`database/README.md`](./database/README.md) for a full file-by-file
breakdown and row counts.

**Option B — Docker Compose** (one command, no local MySQL install; see
[Docker / deployment](#docker--deployment) below) — this also runs all
eight scripts automatically on first start.

### 3. Backend setup

```bash
cd school-backend
# optional: override defaults via env vars (see table below)
export DB_URL="jdbc:mysql://localhost:3306/school_management_system?useSSL=false&serverTimezone=UTC"
export DB_USERNAME=root
export DB_PASSWORD=root
export JWT_SECRET="a-long-random-base64-string"

./mvnw spring-boot:run
```

The API starts on **http://localhost:8080**, with all endpoints under the
`/api/v1` base path (a prefix baked into each controller's
`@RequestMapping`, not a servlet `context-path`).

### 4. Frontend setup

```bash
cd school-frontend
cp .env.example .env
npm install
npm run dev
```

The dev server starts on **http://localhost:5173** (Vite proxies `/api`
requests to `http://localhost:8080` in dev — see `vite.config.ts` — while
`VITE_API_BASE_URL` in `.env` governs the base URL the app's axios client
targets directly).

## Environment variables

### Backend (`school-backend`)

All of these are read in `src/main/resources/application.yml` via Spring's
`${VAR:default}` placeholder syntax.

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile (`dev` enables SQL logging + Swagger UI; `prod` disables both). |
| `DB_URL` | `jdbc:mysql://localhost:3306/school_management_system?useSSL=false&serverTimezone=UTC` | JDBC connection URL. |
| `DB_USERNAME` | `root` | Database username. |
| `DB_PASSWORD` | `root` | Database password. |
| `JWT_SECRET` | (dev-only baked-in default) | Base64 HMAC signing key for access/refresh JWTs — always override outside local dev. |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host for `spring-boot-starter-mail`. |
| `MAIL_PORT` | `587` | SMTP port. |
| `MAIL_USERNAME` | *(empty)* | SMTP username. |
| `MAIL_PASSWORD` | *(empty)* | SMTP password. |
| `MAIL_FROM` | `noreply@school.edu` | "From" address used on outgoing mail. |
| `FRONTEND_URL` | `http://localhost:5173` | Public frontend URL, used to build links in outgoing emails. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed CORS origin(s) for the frontend. |
| `UPLOAD_DIR` | `./uploads` | Filesystem directory for uploaded files (served back under `/uploads/**`). |

Fixed (non-overridable via env) but relevant: server port `8080`;
access-token expiry 900000 ms (15 min); refresh-token expiry 604800000 ms
(7 days); multipart max file/request size 10MB; Swagger UI at
`/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs` (both only when
`springdoc.swagger-ui.enabled`, which the `prod` profile turns off).

### Frontend (`school-frontend`)

From `.env.example` / `.env.development`:

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | Base URL the frontend's axios client calls. **Baked in at build time** by Vite — changing it requires a rebuild, not just a container/process restart. |
| `VITE_APP_NAME` | `Greenwood International School` | Display name shown in the UI (page title, headers). |

## Demo login credentials

All demo accounts are seeded by `database/06_seed_reference_data.sql` /
`07_sample_data.sql` per `SCHEMA_CONTRACT.md`. There are exactly two
passwords across the whole seed set:

| Role | Username | Password |
|---|---|---|
| SUPER_ADMIN | `admin` | `Admin@123` |
| PRINCIPAL | `principal` | `Password@123` |
| VICE_PRINCIPAL | `viceprincipal` | `Password@123` |
| TEACHER (named demo account) | `teacher.demo` | `Password@123` |
| TEACHER (bulk-generated) | `teacher1` … `teacher50` | `Password@123` |
| CLASS_TEACHER | (a subset of the `teacher*` accounts above, promoted to CLASS_TEACHER when assigned a section) | `Password@123` |
| ACCOUNTANT (named demo account) | `accountant.demo` | `Password@123` |
| STAFF / ACCOUNTANT / LIBRARIAN / RECEPTIONIST / SECURITY_GUARD (bulk-generated) | `staff1` … `staff30` | `Password@123` |
| LIBRARIAN (named demo account) | `librarian.demo` | `Password@123` |
| RECEPTIONIST (named demo account) | `receptionist.demo` | `Password@123` |
| SECURITY_GUARD (named demo account) | `security.demo` | `Password@123` |
| STUDENT (bulk-generated) | `student1` … `student500` | `Password@123` |
| PARENT | *(not seeded in this round — `parents`/`student_parents` tables exist but have no rows yet; see `database/README.md` "Notable design choices")* | — |

The `Admin@123` / `Password@123` values above are the plaintext
passwords; the actual `users.password` column stores their bcrypt hashes
(`$2b$10$Ih9JH8QwmYKKqexbzqQRhOz6b8WE3i1QYFOABFL8F6l9pXFOqEP.q` and
`$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa`
respectively), verified against Spring Security's `BCryptPasswordEncoder`.

## API documentation

With the backend running under the `dev` profile (the default), interactive
Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

and the raw OpenAPI JSON at `http://localhost:8080/v3/api-docs`. All
endpoints live under the `/api/v1` base path (e.g.
`http://localhost:8080/api/v1/auth/login`). Both are disabled by the
`prod` Spring profile (`application-prod.yml` sets
`springdoc.swagger-ui.enabled: false` and `springdoc.api-docs.enabled:
false`) — re-enable them there if you need Swagger available in a
production-like environment.

## Docker / deployment

A `docker-compose.yml` at the repo root brings up all three services —
MySQL 8 (auto-seeded), the Spring Boot backend, and the React frontend
served by nginx:

```bash
cp .env.example .env
# edit .env: set MYSQL_ROOT_PASSWORD, JWT_SECRET, and (optionally) mail settings
docker compose up --build
```

- **Frontend**: http://localhost (nginx, published on port 80)
- **Backend / Swagger**: http://localhost:8080 / http://localhost:8080/swagger-ui.html
- **MySQL**: localhost:3306 (also reachable from other containers as host `mysql`)

On the *first* start (empty `mysql_data` volume), the MySQL container
automatically runs every script in `database/` in alphabetical order
(`00_` → `07_`), so the schema, indexes, views, triggers, procedures, and
both seed/sample data sets are loaded with no manual step. Subsequent
`docker compose up` runs reuse the existing volume and skip re-seeding.

Notes specific to this setup:
- `school-backend/Dockerfile` is a two-stage build (Maven+JDK 21 to
  compile, then `eclipse-temurin:21-jre` to run as a non-root user) —
  see that file and `school-backend/README.md` for details.
- `school-frontend/Dockerfile` is also two-stage (`node:20-alpine` to
  build, `nginx:alpine` to serve `dist/`). Because Vite bakes
  `VITE_API_BASE_URL` into the compiled JS at *build* time, the compose
  file passes it as a build `arg` pointing at `http://localhost:8080/api/v1`
  — the address the **browser** (not other containers) will use — not the
  internal `backend` service hostname.
- `school-frontend/nginx.conf` (baked into the frontend image) is the
  simple, single-purpose config that just serves the SPA build with a
  React Router fallback.
- `nginx/nginx.conf` at the repo root is a **separate, standalone
  reference config**, not used by `docker-compose.yml`. It shows how you'd
  front both the frontend static files *and* a reverse-proxied backend
  (`/api/`, `/uploads/`) behind a single Nginx server for a real domain —
  useful as a starting point for a non-compose production deployment
  where you want one public origin instead of two published ports.

## Architecture

Layered, standard Spring Boot MVC structure (package root `com.school.sms`):

```
HTTP request
   │
   ▼
Controller        (@RestController — request mapping, validation trigger)
   │  DTOs (request/*, response/*) in / out
   ▼
Service            (interface) / Service.impl (@Service — business logic,
   │                 transactions)
   ▼
Mapper             (MapStruct — Entity <-> DTO conversion, used by services)
   │
   ▼
Repository         (Spring Data JPA — @Repository interfaces, query methods
   │                 + JPA Specifications in util/specification for
   │                 dynamic/filtered search)
   ▼
Entity             (JPA @Entity classes — 79 of them, 1:1 with the tables
   │                 in SCHEMA_CONTRACT.md)
   ▼
MySQL 8 database   (schema/indexes/views/triggers/procedures from database/)
```

Cross-cutting layers used across the request lifecycle:
- **`config/`** — `SecurityConfig` (JWT filter chain, method security),
  `CorsConfig`, `OpenApiConfig` (Swagger), `JacksonConfig`,
  `PasswordEncoderConfig` (BCrypt), `AsyncConfig`, `AuditorAwareImpl`
  (populates `created_by`/`updated_by`), `WebMvcConfig` (serves
  `/uploads/**` from the `UPLOAD_DIR` filesystem path).
- **`security/`** — `JwtTokenProvider`, `JwtAuthenticationFilter`,
  `CustomUserDetailsService`, `UserPrincipal`, `JwtAuthenticationEntryPoint`
  / `JwtAccessDeniedHandler` (401/403 handling), `StudentAccessGuard`
  (row-level access checks, e.g. a STUDENT/PARENT role only reading their
  own records), `SecurityUtils`.
- **`exception/`** — `GlobalExceptionHandler` (`@RestControllerAdvice`)
  plus `ResourceNotFoundException`, `BadRequestException`,
  `DuplicateResourceException`, `UnauthorizedException`,
  `TokenRefreshException`, mapped to consistent JSON error responses.
- **`util/`** — `AppConstants`, `DateUtil`, `NameUtil`, `GradeResolver`,
  and `util/specification/` (reusable JPA `Specification<T>` builders for
  filtered list/search endpoints across controllers).

Frontend mirrors this by module rather than by layer: each backend
resource has a matching `src/api/*Api.ts` (axios calls), consumed by one
or more pages under `src/pages/<module>/`, with cross-page state in
`src/store/` (Redux Toolkit — auth, notifications, UI/theme) and shared
presentation in `src/components/common` and `src/components/charts`.
Routing (`src/routes/AppRouter.tsx`) wraps role-restricted routes with
`ProtectedRoute` (must be authenticated) and `RoleBasedRoute` (must have
an allowed role), matching the backend's role/permission model.

---

For subproject-specific setup/run/build/test instructions, see
[`school-backend/README.md`](./school-backend/README.md) and
[`school-frontend/README.md`](./school-frontend/README.md).
