# syntax=docker/dockerfile:1
#
# Multi-stage build for the School Management System frontend
# (Vite + React 19 + TypeScript, npm scripts from package.json:
# "build": "tsc -b && vite build", output to dist/).
#
# IMPORTANT: Vite only exposes env vars prefixed VITE_ to client code, and
# it inlines them into the built JS bundle at BUILD time (import.meta.env),
# not at container-start time. That means VITE_API_BASE_URL must be
# supplied as a Docker build ARG (docker-compose.yml does this via the
# frontend service's `build.args`), never as a runtime `environment:`
# entry on the nginx container - setting it at `docker run` time would
# have no effect on an already-built bundle.
#
# Build (from repo root):
#   docker build -t school-frontend -f school-frontend/Dockerfile \
#     --build-arg VITE_API_BASE_URL=http://localhost:8080/api/v1 school-frontend
# Run:
#   docker run -p 80:80 school-frontend

########################################
# Stage 1: install deps and build static assets
########################################
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies first (cached layer as long as lockfile is unchanged).
COPY package.json package-lock.json ./
RUN npm ci

# Copy the rest of the source.
COPY . .

# Baked into the JS bundle at build time - see note above. Defaults to the
# same value as school-frontend/.env.example so a plain `docker build`
# with no --build-arg still produces a working image for local use.
ARG VITE_API_BASE_URL=http://localhost:8080/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

########################################
# Stage 2: serve the static build with nginx
########################################
FROM nginx:alpine AS runtime

# Frontend-specific SPA config (React Router client-side routing) - see
# school-frontend/nginx.conf in this same directory for details.
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
