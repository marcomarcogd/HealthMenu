# HealthMenu

HealthMenu is a nutritionist menu management project split into:

- `src/`: Spring Boot backend for admin APIs, menu rendering, persistence, and AI integration.
- `admin-ui/`: Vue 3 admin console for templates, dictionaries, customers, and menus.
- `example/`: original static reference implementation kept for migration comparison.

## Stack

- Backend: Java 17, Spring Boot 3.4, MyBatis-Plus, MySQL, Thymeleaf
- Frontend: Vue 3, Vite, Element Plus, Pinia
- Tests: JUnit 5 + Spring Boot Test, Vitest

## Repo Layout

- `src/main/java/com/kfd/healthmenu/controller/api/admin`: admin API controllers
- `src/main/java/com/kfd/healthmenu/service/impl`: core business services
- `src/main/resources/sql`: schema and seed data
- `src/main/resources/templates/view`: rendered menu pages
- `admin-ui/src/views`: admin pages
- `admin-ui/src/utils`: tested frontend payload helpers

## Profiles

- `local`:
  default profile for local development, points to MySQL on `localhost`.
- `test`:
  used by automated tests, points to in-memory H2.
- `prod`:
  requires environment variables and disables automatic SQL initialization by default.

## Backend Setup

### Prerequisites

- Java 17
- MySQL 8.x

### Local database

Create a database named `health_menu` before starting the backend, or override the URL with an environment variable.

### Local environment variables

Recommended variables:

```powershell
$env:HEALTH_MENU_DB_URL="jdbc:mysql://localhost:3306/health_menu?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai"
$env:HEALTH_MENU_DB_USERNAME="root"
$env:HEALTH_MENU_DB_PASSWORD=""
$env:HEALTH_MENU_SQL_INIT_MODE="always"
$env:APP_AI_IMPORT_ENABLED="false"
$env:APP_AI_IMPORT_PROVIDER="mock"
```

Recommended database defaults:

- Database charset: `utf8mb4`
- Database collation: `utf8mb4_unicode_ci`
- Keep the local JDBC URL on `utf8mb4` as well. If you use `src/main/resources/application-local.yaml`, make sure its `spring.datasource.url` also includes `characterEncoding=utf8mb4` and `connectionCollation=utf8mb4_unicode_ci`, otherwise Chinese content can become mojibake in the local MySQL database.

Optional share and upload overrides:

```powershell
$env:APP_SHARE_BASE_URL="http://localhost:8080"
$env:APP_UPLOAD_DIR="uploads"
```

### AI / Coze configuration

Do not commit tokens. Configure them through environment variables when needed:

```powershell
$env:APP_AI_IMPORT_ENABLED="true"
$env:APP_AI_IMPORT_PROVIDER="coze"
$env:APP_COZE_ENABLED="true"
$env:APP_COZE_TEXT_IMPORT_URL="https://your-coze-text-endpoint"
$env:APP_COZE_TEXT_IMPORT_TOKEN="your-text-token"
$env:APP_COZE_IMAGE_URL="https://your-coze-image-endpoint"
$env:APP_COZE_IMAGE_TOKEN="your-image-token"
```

### Start backend

```powershell
.\mvnw.cmd spring-boot:run
```

Backend defaults to `http://localhost:8080`.

Notes:

- `/api/admin/**` serves the admin APIs.
- `/view/menu/{id}` and `/share/menu/{token}` serve the rendered menu pages.
- `/` and `/admin` intentionally do not host the admin UI anymore.

## Frontend Setup

Install dependencies:

```powershell
cd admin-ui
npm install
```

Development server:

```powershell
npm run dev
```

Default frontend environment:

- `VITE_ADMIN_API_BASE_URL=/api/admin`
- `VITE_DEV_PROXY_TARGET=http://localhost:8080`

The Vite dev server proxies `/api` to the backend target configured above.

Production build:

```powershell
npm run build
```

## Test Commands

Backend:

```powershell
.\mvnw.cmd test
```

Frontend:

```powershell
cd admin-ui
npm test
```

## Current Workflow

1. Maintain customers, dictionaries, and templates in the admin console.
2. Use template structure to initialize a customer menu.
3. Optionally prefill sections and meals through AI text parsing.
4. Save menu snapshots to the database.
5. Open rendered output through `/view/menu/{id}` or `/share/menu/{token}`.

## Known Gaps

- `MenuPublishRecord` is present in the model but not wired into a full publish flow yet.
- `ExportService` exists, but there is no formal admin export API yet.
- Image upload / AI image generation is only partially modeled and not fully exposed in the UI.
- `example/` remains the migration reference for static HTML and Excel behavior.
