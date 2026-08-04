# Directory Structure

## Overview

这是一个零构建、零依赖的静态 Web App。没有 `package.json`、bundler、npm scripts、模块解析或转译。浏览器直接打开 `index.html` 即可运行；手机局域网访问可用根目录 `server.js`。

所有 JS 文件使用 IIFE 挂到 `window.*` 全局对象。`index.html` 中 `<script>` 标签顺序就是依赖契约。

## Directory Layout

```text
renovation/
├─ index.html              # 单页入口，按顺序加载所有脚本
├─ server.js               # 可选静态服务器，供手机局域网访问
├─ open-app.bat            # 直接打开 index.html
├─ phone-server.bat        # 启动 server.js
├─ css/
│  └─ style.css            # 全部样式，移动端优先
└─ js/
   ├─ data/
   │  ├─ knowledge.js      # window.DATA：流程、指南、风格、百科、空间需求
   │  └─ prices.js         # window.PRICES：价格、预算模板、档位
   ├─ storage.js           # window.Store：localStorage、导入导出、派生任务同步
   ├─ ui.js                # window.UI：转义、格式化、modal、toast、表单
   ├─ views/
   │  ├─ home.js           # 首页仪表盘
   │  ├─ stages.js         # 流程时间线和任务打卡
   │  ├─ budget.js         # 预算和支出
   │  ├─ guide.js          # 指南、验收、风格、百科
   │  └─ more.js           # 我的、空间需求、笔记、联系人、数据管理
   └─ app.js               # window.App：路由、进度、首次向导、全局事件
```

## Module Pattern

模块统一使用：

```js
window.UI = (function () {
  function helper() {}
  return { helper: helper };
})();
```

视图统一使用共享 namespace：

```js
window.Views = window.Views || {};

Views.home = (function () {
  function render(el) {}
  return { render: render };
})();
```

## Load Order

`index.html` 当前加载顺序：

```text
knowledge.js -> prices.js -> storage.js -> ui.js -> views/* -> app.js
```

规则：

- 数据文件必须先于读取它们的视图加载。
- `app.js` 必须最后加载，因为它调用 `App.init()` 并触发首次渲染。
- 新增 view 文件后，脚本标签要放在 `app.js` 前。
- 新增 data 文件后，脚本标签要放在任何消费者前。

## Layer Contract

| Layer | Files | Global | Mutability |
|-------|-------|--------|------------|
| Static data | `js/data/*.js` | `DATA`, `PRICES` | 只读 |
| Persistence | `js/storage.js` | `Store` | 读写 |
| UI helpers | `js/ui.js` | `UI` | 无业务状态 |
| Views | `js/views/*.js` | `Views.*` | 渲染和事件 |
| App shell | `js/app.js` | `App` | 路由和派生计算 |

视图可以读取 `DATA`、`PRICES`、`Store.state`，但不能修改 `DATA` 或 `PRICES`。用户状态变化写入 `Store.state`，然后 `Store.save()`。

## Naming

- JS/CSS 文件使用小写短名，例如 `storage.js`、`budget.js`。
- 全局对象使用现有名称：`DATA`、`PRICES`、`Store`、`UI`、`Views`、`App`。
- 持久化 id 必须稳定。任务 id、预算分类 id、风格 id、验收清单 id 已进入用户 localStorage 和备份文件，不能随意重命名。

## Adding a Feature

按功能归位：

- 新静态装修知识：改 `js/data/knowledge.js`。
- 新预算算法或价格档位：改 `js/data/prices.js`。
- 新持久字段、导入导出、派生任务同步：改 `js/storage.js`。
- 新通用弹窗/格式化/helper：改 `js/ui.js`。
- 新页面或页面内功能：改对应 `js/views/*.js`，必要时在 `js/app.js` 接路由。
- 新视觉样式：改 `css/style.css`。

不要新增目录或框架，除非 PRD 明确改变项目架构。
