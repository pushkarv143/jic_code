# school-frontend

React 19 + TypeScript SPA for the School Management System, built with
Vite and MUI.

For the full project overview, feature list, architecture, demo
credentials, and Docker/deployment instructions, see the
[root README](../README.md) and [`SCHEMA_CONTRACT.md`](../SCHEMA_CONTRACT.md).
This file only covers running/building this subproject on its own.

## Prerequisites

- Node.js 20+ and npm
- The backend running and reachable (default `http://localhost:8080`) —
  see [`../school-backend/README.md`](../school-backend/README.md)

## Setup

```bash
npm install
cp .env.example .env
```

`.env` sets:

| Variable | Default | Notes |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | Base URL the app's axios client calls. Baked into the built JS at build time — changing it means rebuilding, not just restarting. |
| `VITE_APP_NAME` | `Greenwood International School` | Display name used in the UI. |

## Run the dev server

```bash
npm run dev
```

Starts on **http://localhost:5173**. `vite.config.ts` also proxies `/api`
requests to `http://localhost:8080` in dev mode, in addition to whatever
`VITE_API_BASE_URL` is set to.

## Build for production

```bash
npm run build
```

Runs `tsc -b && vite build` and outputs static assets to `dist/`. Preview
the production build locally with:

```bash
npm run preview
```

## Lint / format

```bash
npm run lint
npm run format
```

## Docker

```bash
docker build -t school-frontend -f Dockerfile \
  --build-arg VITE_API_BASE_URL=http://localhost:8080/api/v1 .
docker run -p 80:80 school-frontend
```

Two-stage build: `node:20-alpine` compiles `dist/`, then `nginx:alpine`
serves it using the `nginx.conf` in this directory (SPA fallback for
React Router). See the root [`docker-compose.yml`](../docker-compose.yml)
for the wired-up multi-container setup, and the root
[`nginx/nginx.conf`](../nginx/nginx.conf) for a reference config that
fronts both the frontend and backend behind one reverse proxy.
