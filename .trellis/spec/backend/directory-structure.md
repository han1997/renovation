# Directory Structure

## Runtime Boundary

`server.js` 是唯一服务端文件，位于仓库根目录。它是手机局域网访问用的零依赖静态服务器，不拥有业务模块、路由层、控制器、服务层或数据库层。

参考文件：

- `server.js`
- `phone-server.bat`
- `index.html`

## File Layout

```text
renovation/
├─ server.js          # 可选静态服务器，监听 8787
├─ phone-server.bat   # 启动 server.js
├─ index.html         # 静态应用入口
├─ css/style.css      # 全量样式
└─ js/**/*.js         # 浏览器端业务逻辑
```

## Server Responsibilities

`server.js` 只做四件事：

- 把 `/` 映射到 `/index.html`。
- 根据文件扩展名返回基础 MIME 类型。
- 使用 `path.join(ROOT, ...)` 将请求限制在仓库目录内。
- 在启动时打印本机和局域网访问地址。

## Do Not Add

不要在 `server.js` 中加入：

- API 业务路由。
- 用户数据持久化。
- 登录、鉴权或 session。
- Express/Koa 等框架依赖。
- 构建或热更新逻辑。

如果未来确实需要后端功能，应先创建新的任务并重新设计 spec，而不是把功能塞进当前静态服务器。
