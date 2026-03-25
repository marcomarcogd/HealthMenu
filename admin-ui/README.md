# 后台前端说明

`admin-ui` 是 `HealthMenu` 的后台管理端，基于 Vue 3 + Vite 构建，主要负责以下功能：

- 登录与退出
- 工作台首页
- 客户管理
- 餐单管理
- 模板设计
- 字典管理
- 账号管理
- 角色权限管理

## 常用脚本

```powershell
npm install
npm run dev
npm run build
npm test
```

## 默认环境变量

仓库默认值：

```env
VITE_ADMIN_API_BASE_URL=/api/admin
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

建议本地覆盖文件：

- `admin-ui/.env.local`

示例：

```env
VITE_ADMIN_API_BASE_URL=/api/admin
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

## 开发说明

- 本前端默认独立部署，不再依赖后端根路径页面。
- 本地开发时，Vite 会把 `/api` 请求代理到后端。
- 后端环境变量、数据库字符集、Coze 配置等公共说明，请查看仓库根目录 `README.md`。
