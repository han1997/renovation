# Logging Guidelines

## Current Logging

本项目没有日志系统。`server.js` 只在启动时用 `console.log()` 打印：

- 应用已启动提示。
- 电脑访问地址 `http://localhost:8787`。
- 每个非内部 IPv4 网卡对应的手机访问地址。
- 关闭窗口即停止服务的说明。

参考文件：`server.js`

## Browser Diagnostics

浏览器端只在本地存储失败时使用 `console.warn()`：

- `js/storage.js` 读取 localStorage 失败。
- `js/storage.js` 保存 localStorage 失败。

用户可见反馈使用 `UI.toast()`，不是日志。

## Rules

- 不要引入日志库。
- 不要记录用户的装修预算、电话、笔记、备份内容等隐私数据。
- 不要在正常交互路径加入高频 `console.log()`。
- 启动日志可以继续保持中文提示加 URL，方便非开发用户照着打开手机浏览器。
