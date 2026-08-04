# Database Guidelines

## Current State

本项目没有数据库、ORM、迁移、服务端模型或后端查询层。

用户数据保存在浏览器 `localStorage` 中，入口是 `js/storage.js` 的 `Store.state`。备份和迁移通过 JSON 导出/导入完成，CSV 只用于支出明细导出。

参考文件：

- `js/storage.js`
- `js/views/more.js`
- `js/views/budget.js`

## Persistence Rule

不要新增服务端数据库来保存当前用户数据。这个应用的产品约束是离线可用、双击即用、各设备浏览器数据彼此独立。

## Schema Changes

新增持久字段时，在 `js/storage.js` 的 `defaults()` 中补齐默认值，并在 `load()` 中兼容旧备份缺失字段的情况。

现有模式示例：

- `profile` 可为 `null`。
- `budget.categories` 和 `budget.expenses` 必须保持数组。
- `tasksDone`、`taskDates`、`stageOverride`、`checks` 必须保持对象。

## Forbidden Patterns

- 不要引入 SQLite、IndexedDB、远程数据库或云同步，除非 PRD 明确改变离线产品定位。
- 不要只在视图层临时创建新字段而不更新 `defaults()`。
- 不要重命名已持久化的 id，例如 task id、budget category id、style id；这些值会出现在用户已有备份和 localStorage 中。
