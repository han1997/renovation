# Error Handling

## Static Server Errors

`server.js` 当前使用最小错误处理：

- 路径越界返回 `403 Forbidden`。
- 文件读取失败返回 `404 Not Found`。
- 成功读取文件时按扩展名返回 MIME 类型，不识别则用 `application/octet-stream`。

参考文件：`server.js`

## Browser Data Errors

主要错误处理在浏览器端：

- `js/storage.js` 捕获 `localStorage` 读取和保存异常。
- JSON 导入失败时通过回调返回错误文案。
- `js/app.js` 在 `Store.storageOk === false` 时显示 toast，提示隐私模式等无法持久保存的情况。

## Local Pattern

对用户可恢复的错误，使用 `UI.toast()` 或 `UI.formModal()` 的校验状态，不要让异常直接打断渲染。

对静态文件服务器错误，保持纯文本状态响应即可。不要为当前服务器引入统一 JSON 错误格式，因为项目没有 API 客户端。

## Common Mistakes

- 视图渲染中直接读取不存在的 `DATA.*` 字段会在 render 时抛错，新增数据消费者前先确认 `js/data/knowledge.js` 或 `js/data/prices.js` 已定义字段。
- 字符串拼接 HTML 时忘记 `UI.esc()` 会带来注入风险，尤其是用户输入的 note、contact、expense、space name。
- 修改 `server.js` 路径处理时不能削弱 `filePath.startsWith(ROOT)` 这类目录逃逸防护。
