# Deploy check

Verify FE Pages + BE cloud wiring for Opportunity Board.

1. Confirm Vite `base` and `asset()` usage so GH Pages assets load.
2. Confirm DigitalOcean source dir is `backend` and secrets match `.env.example`.
3. Confirm GitHub Variable `VITE_API_URL` points at the live API `/api/v1`.
4. Report the Pages URL and API health endpoint status.
