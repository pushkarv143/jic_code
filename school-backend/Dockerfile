# syntax=docker/dockerfile:1
#
# Multi-stage build for the School Management System backend
# (Spring Boot 3.3.4 / Java 21, Maven, groupId com.school.sms,
# artifactId school-backend, <finalName>school-backend</finalName>
# -> build produces target/school-backend.jar).
#
# Build (from repo root):
#   docker build -t school-backend -f school-backend/Dockerfile school-backend
# Run:
#   docker run -p 8080:8080 --env-file .env school-backend

########################################
# Stage 1: build the jar with Maven
########################################
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Copy the Maven wrapper and POM first so the dependency-resolution layer
# is cached independently of application source changes.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline || true

# Now copy the actual source and build.
COPY src src
RUN ./mvnw -B -DskipTests package

########################################
# Stage 2: slim runtime image
########################################
FROM eclipse-temurin:21-jre AS runtime

# Run as a dedicated non-root user rather than the container default root.
RUN groupadd --system spring && useradd --system --gid spring --home /app --shell /usr/sbin/nologin spring

WORKDIR /app

# pom.xml sets <finalName>school-backend</finalName>, so the Spring Boot
# Maven plugin repackages target/school-backend.jar (fat jar) - copy that
# exact artifact, not target/*.jar, to avoid also grabbing the
# .original thin jar that the plugin leaves alongside it.
COPY --from=build /build/target/school-backend.jar app.jar

# Default UPLOAD_DIR (see application.yml: app.upload.dir, env UPLOAD_DIR,
# defaults to ./uploads which resolves relative to the working directory,
# i.e. /app/uploads here). Pre-create and hand ownership to the app user;
# docker-compose.yml mounts a named volume on top of this path.
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
