# Quality Guidelines

## Scope

这些规则只适用于 `server.js` 和启动脚本。业务功能改动通常属于 frontend spec。

## Required Checks

修改 `server.js` 后：

```bash
node server.js
```

确认：

- 控制台打印 localhost 和局域网 IPv4 地址。
- `http://localhost:8787` 能加载首页。
- CSS 和 JS 文件能正常返回。
- 不存在路径穿越漏洞，例如 `../` 请求不能读到仓库外文件。

## Code Style

- 保持 CommonJS 和 Node 内置模块：`require('http')`、`require('fs')`、`require('path')`、`require('os')`。
- 保持零依赖，不新增 `package.json` 只为静态服务。
- MIME 表放在顶部常量中，新增静态资源类型时只扩展 `MIME`。
- 继续使用固定端口 `8787`，除非任务明确要求可配置端口。

## Forbidden Patterns

- 不要把用户数据写到服务器磁盘。
- 不要接受上传、执行命令或代理外部 URL。
- 不要把请求路径直接拼成文件路径；必须规范化并限制在 `ROOT` 下。
- 不要让静态服务器成为应用业务状态来源。
