---
name: deploy-opportunity-board
description: Use when deploying Opportunity Board FE to GitHub Pages or BE to DigitalOcean/GitHub Releases, or when wiring VITE_API_URL and cloud env secrets.
---

# Deploy Opportunity Board

## FE (GitHub Pages)

1. Confirm `.github/workflows/deploy.yml` is on `main`.
2. Settings → Pages → Source = GitHub Actions.
3. Set Actions Variable `VITE_API_URL` to `https://<api-host>/api/v1`.
4. Push `main` or re-run **Deploy — Pages + Releases**.
5. Open `https://khanhtm45.github.io/opportunity_borad/`.

## BE (DigitalOcean App Platform)

1. Create App from GitHub repo `khanhtm45/opportunity_borad`, branch `main`.
2. Source Directory = `backend` (Dockerfile at `backend/Dockerfile`).
3. HTTP port `8080`. Secrets: `DB_PASSWORD`, `JWT_SECRET` (see `.env.example`).
4. After live URL exists, update `VITE_API_URL` and redeploy Pages.

## BE artifact (GitHub Releases)

- Same `deploy.yml` job `release-backend` publishes `opportunity-board-backend-*.jar` as tag `backend-<run_number>`.
- Releases do **not** run the server.

## Verify

```bash
curl -sI https://khanhtm45.github.io/opportunity_borad/
curl -s https://<api-host>/api/v1/categories
npx harness-score --min-level 4
```
