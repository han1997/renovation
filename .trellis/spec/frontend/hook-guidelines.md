# Hook Guidelines

## Current State

本项目没有 React、Vue、Svelte 或任何 hooks 系统。不要新增 `use*` hooks、React 组件或框架运行时。

## Where Shared Logic Lives

可复用逻辑按现有全局模块归位：

- UI 格式化、弹窗、toast、下载：`js/ui.js`
- 持久化、默认状态、导入导出、派生任务同步：`js/storage.js`
- 路由、进度计算、首次向导、跨视图 helper：`js/app.js`
- 静态知识和枚举：`js/data/knowledge.js`
- 价格、预算估算、档位名称：`js/data/prices.js`

## Stateful View Logic

少量视图内部 UI 状态保存在 IIFE 闭包变量中，例如：

- `js/views/budget.js` 的 `sub` 和 `filterCat`。
- `js/views/guide.js` 的 `sub`、`query`、`topic`、`acceptOpen`。
- `js/views/stages.js` 的 `expandedId`。

只有不需要跨页面持久保存的 UI 状态才放闭包变量。用户数据必须进入 `Store.state`。

## Data Fetching

没有网络数据获取。所有知识库和价格参考都来自 `js/data/*.js` 的静态数组。新增资料时优先扩展这些数据文件，并同步更新消费者视图。

## Anti-Patterns

- 不要引入 React 只为使用 hooks。
- 不要把持久用户数据藏在视图闭包变量中。
- 不要新增异步 fetch 依赖外部服务，除非 PRD 明确改变离线可用定位。
- 不要创建新的全局 helper 前不先搜索 `UI`、`Store`、`App` 是否已有相同能力。
