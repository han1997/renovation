# Component Guidelines

## Component Model

本项目没有 React/Vue 组件。组件形态是“返回 HTML 字符串的 render helper”加“全局事件委托”。

参考文件：

- `js/views/home.js`
- `js/views/stages.js`
- `js/views/budget.js`
- `js/views/guide.js`
- `js/views/more.js`
- `js/ui.js`

## View File Shape

每个视图文件遵循：

```js
window.Views = window.Views || {};

Views.budget = (function () {
  function render(el, param) {
    el.innerHTML = html;
  }

  function onAction(action, el) {}
  function onChange(kind, el) {}

  return { render: render, onAction: onAction, onChange: onChange };
})();
```

新增视图时保持这个结构，并在 `index.html` 中把脚本放在 `app.js` 前。

## Rendering Rules

- 渲染入口统一是 `render(el, param)`，最后设置 `el.innerHTML`。
- 可复用片段写成本文件内 helper，例如 `renderOverview()`、`renderList()`、`stageHTML()`。
- 用户输入或可变状态进入 HTML 前必须用 `UI.esc()`。
- 数字金额用 `UI.money()` 或 `UI.moneyFull()`。
- 日期用 `UI.today()`、`UI.dateCN()`、`UI.daysFromToday()`。
- 长列表排序和过滤在 render helper 内完成，避免在 `DATA` 原数组上原地修改。

## Event Pattern

`js/app.js` 在 document 上委托 click/change：

- 点击命令使用 `data-action`。
- 表单变更使用 `data-change`。
- tab 使用 `data-tab`。
- 导航使用 `data-action="nav"`、`data-target`、`data-param`。

视图不要直接给主页面大量绑定 click listener。只有渲染后必须绑定的局部交互才在视图内绑定，例如 `guide.js` 的搜索输入和 quiz modal。

## Modal and Forms

优先使用 `UI.formModal()` 构建标准表单，参考 `budget.js` 的支出表单和 `more.js` 的联系人/笔记表单。

使用 `UI.modal()` 时：

- 标题和 body 中的用户数据要转义。
- 破坏性操作先用 `UI.confirmDlg()` 二次确认。
- 保存后调用 `Store.save()`，再关闭弹窗并 `App.rerender()` 或 `App.go()`。

## Styling

复用 `css/style.css` 已有类：`.card`、`.btn`、`.btn-primary`、`.seg`、`.chip`、`.badge`、`.fold`、`.field`、`.empty`。新增样式应放在 `style.css` 中对应主题区块附近，不要写分散的新 CSS 文件。

## Common Mistakes

- 在 HTML 字符串中直接拼接用户输入。
- 给动态列表每一项手动绑定 click，而不是使用 `data-action` 委托。
- 修改 `DATA` 或 `PRICES` 静态数据对象来保存用户状态。
- 新增按钮但忘记在当前 view 的 `onAction()` 处理。
- 新增 change 控件但忘记导出 `onChange` 或使用 `data-change`。
