# HealthMenu 营养餐单管理系统

`HealthMenu` 是一个面向营养师/健管师的餐单管理项目，当前已经拆分为后端服务和独立后台前端两部分：

- `src/`：Spring Boot 后端，负责后台接口、鉴权、数据持久化、餐单展示页渲染、AI/Coze 集成等能力。
- `admin-ui/`：Vue 3 后台管理端，负责客户、模板、字典、餐单、账号、角色权限等操作界面。
- `example/`：最初的静态参考实现，保留用于迁移对照，不再作为正式运行入口。

## 当前已具备的核心能力

- 后台登录鉴权，已发布菜单可免登录查看，未发布菜单仅后台登录后可预览。
- 客户、模板、字典、餐单的完整后台管理流程。
- 餐单分页查询、筛选、批量发布、批量导出。
- AI 文本解析、AI 生图，并支持图片下载到本地后再入库。
- 账号管理、角色管理、权限配置、账号审计日志。
- 餐单展示页分享链接、导出图片、本地图片渲染。

## 技术栈

- 后端：Java 17、Spring Boot 3.4、MyBatis-Plus、MySQL、Thymeleaf
- 前端：Vue 3、Vite、Element Plus、Pinia
- 测试：JUnit 5 + Spring Boot Test、Vitest

## 仓库结构

- `src/main/java/com/kfd/healthmenu/controller/api/admin`：后台管理接口
- `src/main/java/com/kfd/healthmenu/controller/api/auth`：登录、当前用户、退出登录
- `src/main/java/com/kfd/healthmenu/service/impl`：核心业务实现
- `src/main/resources/sql`：建表与初始化数据
- `src/main/resources/templates/view`：餐单成品展示页模板
- `admin-ui/src/views`：后台页面
- `admin-ui/src/api`：前端接口封装
- `admin-ui/src/stores`：登录态与权限状态管理

## 运行环境

### 后端前置条件

- Java 17
- MySQL 8.x

### 本地数据库

启动前请先创建名为 `health_menu` 的数据库，或通过环境变量覆盖连接地址。

建议数据库默认配置：

- 数据库字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`

注意：

- MySQL 表和数据库请使用 `utf8mb4`
- JDBC 的 `characterEncoding` 不要写成 `utf8mb4`
- MySQL Connector/J 这里应使用 Java 字符集名，所以建议写 `characterEncoding=UTF-8`

## 后端环境变量

推荐本地开发变量：

```powershell
$env:HEALTH_MENU_DB_URL="jdbc:mysql://localhost:3306/health_menu?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai"
$env:HEALTH_MENU_DB_USERNAME="root"
$env:HEALTH_MENU_DB_PASSWORD=""
$env:HEALTH_MENU_SQL_INIT_MODE="always"
$env:APP_AI_IMPORT_ENABLED="false"
$env:APP_AI_IMPORT_PROVIDER="mock"
```

可选的分享与上传目录覆盖：

```powershell
$env:APP_SHARE_BASE_URL="http://localhost:8080"
$env:APP_UPLOAD_DIR="uploads"
```

生产环境日志文件可单独指定：

```powershell
$env:APP_LOG_FILE="/opt/healthmenu/logs/healthmenu.log"
```

## Coze / AI 配置

请不要把 Token 提交回仓库，统一通过环境变量注入：

```powershell
$env:APP_AI_IMPORT_ENABLED="true"
$env:APP_AI_IMPORT_PROVIDER="coze"
$env:APP_COZE_ENABLED="true"
$env:APP_COZE_TEXT_IMPORT_URL="https://your-coze-text-endpoint"
$env:APP_COZE_TEXT_IMPORT_TOKEN="your-text-token"
$env:APP_COZE_IMAGE_URL="https://your-coze-image-endpoint"
$env:APP_COZE_IMAGE_TOKEN="your-image-token"
```

## 启动后端

```powershell
.\mvnw.cmd spring-boot:run
```

默认地址：

- 后端服务：`http://localhost:8080`
- 后台接口前缀：`http://localhost:8080/api/admin`

补充说明：

- `/api/admin/**`：后台接口
- `/api/auth/**`：登录相关接口
- `/api/public/**`：对外公开接口
- `/view/menu/{id}`、`/share/menu/{token}`：餐单展示页
- `/` 与 `/admin` 已不再托管后台前端页面

## 前端开发

安装依赖：

```powershell
cd admin-ui
npm install
```

启动开发环境：

```powershell
npm run dev
```

默认前端环境变量：

- `VITE_ADMIN_API_BASE_URL=/api/admin`
- `VITE_DEV_PROXY_TARGET=http://localhost:8080`

Vite 开发服务器会把 `/api` 代理到上述后端地址。

生产构建：

```powershell
npm run build
```

## 测试命令

后端测试：

```powershell
.\mvnw.cmd test
```

前端测试：

```powershell
cd admin-ui
npm test
```

## 生产日志

生产环境默认会把后端日志写到：

- `/opt/healthmenu/logs/healthmenu.log`

日志策略：

- 默认级别：`ERROR`
- 单文件最大：`20MB`
- 保留历史：`14` 份
- 总大小上限：`500MB`

如果你沿用 `systemd` 启动，还需要提前创建日志目录：

```bash
sudo mkdir -p /opt/healthmenu/logs
sudo chown -R $(whoami):$(whoami) /opt/healthmenu/logs
```

如果服务是以专门账号运行，请把上面的属主改成对应运行账号。

## 默认账号

系统首次启动时，如果数据库中还没有账号，会自动补齐两个默认账号：

- `admin / Admin@123456`
- `manager / Manager@123456`

这些默认密码只用于初始化，请在实际使用前尽快修改。

## 当前角色与权限

当前内置角色：

- 管理员
- 健管师

系统已经支持：

- 独立角色管理页面
- 为角色配置权限点
- 账号绑定角色
- 登录态按最新角色权限实时刷新
- 账号审计日志

## 已知限制

- 权限点目前仍是“模块级”粒度，还没有细分到“查看/编辑/发布/导出”等操作级。
- 前端生产包里仍有较大的公共依赖 chunk，构建会出现体积告警，但当前不影响运行。
- `example/` 目录仍是静态参考实现，正式功能请以 `admin-ui/` + 后端接口为准。

## 开发注意事项

- 全局统一使用 UTF-8，避免中文乱码。
- 数据库敏感信息、Coze Token、本地私有配置不要提交进仓库。
- 如果修改角色或权限，请同步关注后台菜单显示、路由守卫和后端接口鉴权是否一致。
