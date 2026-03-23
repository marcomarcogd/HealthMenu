# Help

Primary project documentation now lives in `README.md`.

Quick commands:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
cd admin-ui
npm install
npm run dev
npm test
```

Important defaults:

- Backend profile defaults to `local`
- Backend API base is `http://localhost:8080/api/admin`
- Frontend Vite proxy defaults to `http://localhost:8080`

Do not place database passwords or Coze tokens back into committed YAML files. Use environment variables described in `README.md`.
