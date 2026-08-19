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

## Routing and Scroll

`App.go(tab, param, opts)` 负责切换视图和滚动策略：

- 底部 tab 切换或无参数跨 tab 导航可以回到顶部。
- 带 `param` 的页面内目标导航不要先回到顶部，应让目标 view 自己滚动到锚点。
- 流程页阶段展开和带阶段 `param` 的导航应由 `Views.stages` 聚焦到舒适阅读位置，阶段标题约在视口上方 20%-25%，不要贴顶。
- 同一 tab 内的交互优先使用 `App.rerender()` 保留当前位置。
- `data-action="nav"` 默认按是否存在 `data-param` 选择滚动策略；新增导航入口时不要手写 `window.scrollTo(0, 0)`。
- `current.param` 是一次性渲染信号：`render()` 读取后应清空，再把本次参数传给 view，避免后续 `App.rerender()` 重放旧锚点并覆盖用户当前展开/滚动状态。

```js
var param = current.param;
current.param = null;
if (v) v.render(viewEl, param);
```

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

## 首页行动中心契约

首页的行动中心是任务状态的展示与快速操作层，不新增独立任务状态：

- 日期任务只收集未完成任务，并按逾期、今天、未来 7 天分组；每组最多展示 3 项。
- 只有不存在任何未完成日期任务时，才回退展示当前阶段前 3 个未完成任务。
- 行动项复选框使用 `data-change="task"`，处理后必须修改 `Store.state.tasksDone`、调用 `Store.save()`，再调用 `App.rerender()`。
- 行动项文字沿用 `data-action="nav"` 跳转流程阶段；复选框和其 `label` 不能触发父级导航。
- 首页风险条只展示最严重的一个预算分类超支和当前阶段的采购入口，不复制预算或建材日历的完整列表逻辑。

这样可以保证首页快速操作与流程页共享同一状态来源，同时避免逾期任务过多时遮蔽今天和近期任务。

## 流程页阶段摘要契约

流程页顶部的阶段摘要与时间轴必须共享同一个目标阶段上下文：

- 有效的阶段路由参数优先决定摘要、默认展开阶段和聚焦目标；无参数时使用 `App.currentStage()`。
- 非法阶段参数必须安全回退到当前阶段，并清除旧的展开/聚焦状态，不能沿用上一次查看的阶段。
- 摘要中的“下一步行动”只触发 `toggle-stage` 展开和聚焦，不复制任务完成控件；任务完成仍在阶段清单中处理。
- 摘要触发 `App.rerender()` 后，如仍需保持指定阶段，必须通过一次性内部上下文传递阶段 ID，不能依赖已被 `App.render()` 消费的路由参数。
- 采购和验收摘要入口只负责导航到现有指南页面，阶段摘要不得复制完整清单或新增持久化状态。

这样可以避免从首页带阶段参数进入后摘要跳回全局当前阶段，也能避免重新渲染时聚焦目标丢失。
