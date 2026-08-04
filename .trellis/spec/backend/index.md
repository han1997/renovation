# Backend Development Guidelines

> 本项目没有业务后端。这里的 backend 规范只覆盖根目录 `server.js` 这个可选的 Node.js 静态文件服务器。

## Overview

应用主体是可直接打开的静态 Web App：`index.html`、`css/style.css`、`js/**/*.js`。`server.js` 只用于局域网手机访问，使用 Node 内置模块 `http` / `fs` / `path` / `os`，没有 Express、数据库、登录、接口或服务端业务状态。

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | 静态服务文件边界和目录约定 | Filled |
| [Database Guidelines](./database-guidelines.md) | 明确本项目没有数据库层 | Filled |
| [Error Handling](./error-handling.md) | 静态服务和浏览器端错误处理约定 | Filled |
| [Quality Guidelines](./quality-guidelines.md) | Node 静态服务改动检查 | Filled |
| [Logging Guidelines](./logging-guidelines.md) | `server.js` 控制台输出约定 | Filled |

## Cross-Layer Rule

不要把业务逻辑迁移到 `server.js`。用户数据仍保存在浏览器 `localStorage` 中，导入导出由 `js/storage.js` 处理。`server.js` 只负责把仓库内静态文件安全地返回给浏览器。

## Verification

后端相关改动至少运行：

```bash
node server.js
```

然后访问 `http://localhost:8787`，确认页面能加载 `index.html`、`css/style.css` 和 `js/**/*.js`。
