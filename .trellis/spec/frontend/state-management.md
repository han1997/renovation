# State Management

## Source of Truth

用户状态的唯一来源是 `js/storage.js` 中的 `Store.state`。它从 `localStorage` key `zhuangxiu_guanjia_v1` 读取，并通过 `Store.save()` 延迟写回。

参考文件：

- `js/storage.js`
- `js/app.js`
- `js/views/stages.js`
- `js/views/budget.js`
- `js/views/more.js`

## State Shape

`defaults()` 定义持久状态结构：

- `profile`: 房屋与装修基础信息，可为 `null`。
- `tasksDone`: `{ taskId: true }`。
- `taskDates`: `{ taskId: 'YYYY-MM-DD' }`。
- `customTasks`: 用户自定义任务和派生任务。
- `stageOverride`: 整阶段手动完成标记。
- `budget.categories` / `budget.expenses`: 预算分类和支出。
- `checks`: 验收清单勾选。
- `notes` / `contacts`: 我的页数据。
- `spaces`: 空间需求。
- `quiz`: 风格测试结果。

新增持久字段必须先更新 `defaults()`，并在 `load()` 后补齐旧数据兼容。

## Mutation Pattern

现有代码直接修改 `Store.state`，然后调用 `Store.save()`：

```js
Store.state.budget.expenses.push({ id: Store.uid('ex'), name: vals.name });
Store.save();
App.rerender();
```

保持这个模式。不要引入 Redux、MobX、Immer 或深拷贝状态管理。

## Derived State

跨数据派生逻辑放在 `App` 或 `Store`：

- `App.stageTasks(stage)` 合并静态阶段任务和 `customTasks`。
- `App.stageProgress(stage)` 根据 task 完成状态和 stage override 计算进度。
- `Store.syncSpaceTasks(space)` 从空间需求派生阶段任务。
- `Store.removeSpaceTasks(spaceId)` 删除空间需求时清理派生任务。

不要在多个视图里复制同一套进度或派生任务计算。

## Preserve User History

空间需求删除或重算时，已打卡的派生任务会转成普通自定义任务，只删除 `spaceId`，避免丢失历史进度。未来新增类似“领域对象派生任务”的功能应复用这个策略。

## Storage Failure

`Store.storageOk` 表示 localStorage 是否可用。隐私模式等保存失败时，`App.init()` 会提示用户。新增保存路径时不要绕过 `Store.save()`。

## Common Mistakes

- 修改 `Store.state` 后忘记 `Store.save()`。
- 保存后忘记 `App.rerender()`，导致界面仍显示旧状态。
- 重命名已持久化 id，破坏旧 localStorage 和备份。
- 从视图直接改 `DATA.stages`、`DATA.styles` 或 `PRICES.rates`。
