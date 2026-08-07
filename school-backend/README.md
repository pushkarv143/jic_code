# school-backend

Spring Boot 3.3.4 / Java 21 REST API for the School Management System
(Maven, groupId `com.school.sms`, artifactId `school-backend`, base
package `com.school.sms`).

For the full project overview, feature list, architecture diagram,
demo credentials, and Docker/deployment instructions, see the
[root README](../README.md) and [`SCHEMA_CONTRACT.md`](../SCHEMA_CONTRACT.md).
This file only covers running/building this subproject on its own.

## Prerequisites

- Java 21 (JDK)
- A running MySQL 8 instance with the schema loaded (see
  [`../database`](../database) — run `00_create_database.sql` through
  `07_sample_data.sql` in order)
- Maven itself is **not** required — use the included wrapper (`mvnw` /
  `mvnw.cmd`), which downloads Maven 3.9.9 automatically.

## Configuration

All config is in `src/main/resources/application.yml` (plus
`application-dev.yml` / `application-prod.yml` profile overrides), driven
entirely by environment variables with sensible local defaults:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_HOST`,
`MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`,
`FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`, `UPLOAD_DIR`,
`SPRING_PROFILES_ACTIVE` (`dev` by default). See the root README's
[environment variables table](../README.md#environment-variables) for
defaults and descriptions.

## Run locally

```bash
./mvnw spring-boot:run
```

(On Windows: `mvnw.cmd spring-boot:run`.) The API starts on
**http://localhost:8080**; every endpoint is under the `/api/v1` base
path (set per-controller via `@RequestMapping`, not a servlet
context-path). Swagger UI: `http://localhost:8080/swagger-ui.html`
(enabled under the default `dev` profile; disabled under `prod`).

To override config, export env vars first, e.g.:

```bash
export DB_URL="jdbc:mysql://localhost:3306/school_management_system?useSSL=false&serverTimezone=UTC"
export DB_USERNAME=root
export DB_PASSWORD=root
export JWT_SECRET="a-long-random-base64-secret"
./mvnw spring-boot:run
```

## Build a jar

```bash
./mvnw -DskipTests package
```

Produces `target/school-backend.jar` (the `<finalName>school-backend</finalName>`
in `pom.xml`, packaged as a Spring Boot fat jar; `target/school-backend.jar.original`
is the unpackaged thin jar left alongside it and can be ignored). Run it with:

```bash
java -jar target/school-backend.jar
```

## Run tests

```bash
./mvnw test
```

## Docker

```bash
docker build -t school-backend -f Dockerfile .
docker run -p 8080:8080 --env-file ../.env school-backend
```

See `Dockerfile` for the two-stage build (JDK 21 build stage, `eclipse-temurin:21-jre`
non-root runtime stage) and the root [`docker-compose.yml`](../docker-compose.yml)
for the wired-up multi-container setup (MySQL + backend + frontend).
