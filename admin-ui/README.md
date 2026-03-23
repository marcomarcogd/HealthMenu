# admin-ui

Vue 3 admin console for HealthMenu.

## Scripts

```powershell
npm install
npm run dev
npm run build
npm test
```

## Environment

Committed defaults:

```env
VITE_ADMIN_API_BASE_URL=/api/admin
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

Recommended local override file:

- `admin-ui/.env.local`

Example:

```env
VITE_ADMIN_API_BASE_URL=/api/admin
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

## Notes

- The frontend is intended to be deployed independently from the backend root page.
- During local development, Vite proxies `/api` to the backend.
- Shared backend setup, profile rules, and environment variable details are documented in the repo root `README.md`.
