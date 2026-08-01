# Opportunity Board — Agent Guide

Student opportunities board: **Spring Boot** API (`backend/`) + **React/Vite** UI (`frontend/`).
DB is Supabase PostgreSQL. FE deploys to GitHub Pages; BE JAR to GitHub Releases and/or DigitalOcean App Platform.

## Project overview

| Area | Path | Stack |
|------|------|--------|
| API | `backend/` | Java 17+/21, Spring Boot 3.3, JWT, JPA |
| Web | `frontend/` | React 18, Vite 5, Tailwind, axios |
| CI | `.github/workflows/` | `ci.yml` (test/Sonar), `deploy.yml` (Pages + Releases) |
| DO | `.do/app.yaml`, `backend/Dockerfile` | App Platform runtime |
| Env templates | `.env.example`, `backend/.env.example`, `frontend/.env.example` | Cloud/local secrets |

## Build & test

```bash
# Backend
cd backend && mvn -B verify

# Frontend
cd frontend && npm ci && npm run build

# Harness maturity
npx harness-score --min-level 4
```

Local FE: open `http://localhost:5173/opportunity_borad/` (Vite `base` is `/opportunity_borad/`).
Local API: `http://localhost:8080` with Vite proxy `/api` → `:8080`.

## Architecture conventions

- Controllers under `backend/src/main/java/.../interface_http/controller`
- Public API prefix: `/api/v1`
- FE HTTP client: `frontend/src/api/client.js` — use `VITE_API_URL` in production
- Public static assets: always via `asset()` from `frontend/src/lib/assets.js` (respect Vite base)
- Never commit `.env` secrets; use GitHub Variables / DigitalOcean Secrets

## Agent non-negotiables

1. Do not invent deploy hosts on GitHub Pages for the Spring Boot API — Pages is static FE only.
2. Keep Sonar `projectKey` as `khanhtm45_opportunity_borad` unless explicitly changing SonarCloud.
3. Prefer small, focused diffs; match existing naming and package layout.
4. `_repos/` is vendored tooling (Hermes/skills) — ignore unless the task names it.

## Useful commands for agents

- Deploy FE Pages + BE release: push `main` → `deploy.yml`
- DigitalOcean: source directory `backend`, Dockerfile `backend/Dockerfile`, port `8080`
- Set GitHub Variable `VITE_API_URL=https://<do-app>.ondigitalocean.app/api/v1` after BE is live
