# Type Safety

## Current State

项目使用普通 JavaScript，没有 TypeScript、JSDoc 类型检查、Zod 或构建期校验。因此类型安全依赖运行时防御和稳定数据形状。

## Runtime Guards

现有防御模式集中在 `js/storage.js`：

- `load()` 合并 `defaults()`，兼容旧备份缺失字段。
- 数组字段用 `Array.isArray()` 兜底。
- 对象字段检查 `typeof === 'object'`。
- JSON 导入要求顶层对象存在且包含 `ver`。

新增持久结构时照这个模式补防御。

## Data Contracts

静态数据字段必须和视图读取保持一致：

- `DATA.stages[].id` 被 `tasksDone`、`taskDates`、`customTasks[].stageId`、`spaces[].stageIds` 引用。
- `DATA.checklists[].id` 被 `stage.acceptIds` 和 guide 验收页引用。
- `DATA.styles[].id` 被 `profile.styleId` 和 `quiz.styleId` 引用。
- `PRICES.rates[].id` 被 `budget.categories[].id`、`expenses[].catId`、`spaces[].budgetCat` 引用。

新增或重命名 id 前必须全文搜索引用。

## Form Values

`UI.formModal()` 返回字符串或数字：

- `type: 'number'` 会 `parseFloat()`，空值为 `null`。
- `required` 字段为空时阻止提交。
- 日期输入仍是 `YYYY-MM-DD` 字符串。

业务层仍需验证范围，例如 `app.js` 首次向导检查面积必须在合理区间。

## HTML Safety

任何来自用户输入、localStorage、导入 JSON 或可编辑字段的值，进入 HTML 字符串前必须 `UI.esc()`。静态数据也建议转义，除非明确需要插入可信 HTML。

## Forbidden Patterns

- 不要依赖隐式 truthy 判断来区分 `0`、空字符串和缺失字段，金额和面积要显式处理。
- 不要新增无法由 `defaults()` 恢复的嵌套结构。
- 不要把数组当对象 map 使用，现有持久字段形状要稳定。
- 不要在导入 JSON 后直接信任所有嵌套字段类型。
