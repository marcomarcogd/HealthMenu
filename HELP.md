# 帮助说明

项目的完整说明请以根目录的 [README.md](C:/Users/vv768/IdeaProjects/HealthMenu/README.md) 为准，这里只保留最常用的快速指令。

## 常用命令

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
cd admin-ui
npm install
npm run dev
npm test
npm run build
```

## 默认约定

- 后端默认 profile：`local`
- 后台接口默认前缀：`http://localhost:8080/api/admin`
- 前端开发代理默认指向：`http://localhost:8080`

## 重要提醒

- 数据库请保持 `utf8mb4`，但 JDBC `characterEncoding` 请使用 `UTF-8`
- 不要把数据库密码、Coze Token、本地私有配置写回已提交的 YAML 文件
- 账号、角色、权限相关改动后，建议同时跑一次前后端测试
