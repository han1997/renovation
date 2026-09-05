# 随手记：快速捕捉购物愿望与阶段备忘，并按分类整理展示

## Goal

用户在装修过程中会随时想起想购买的家具/家电/生活用品，或某个施工阶段的注意事项。需要一个**低摩擦的快速记录入口**，以及一个**分类整理后的直观展示区**，让随手记的内容不散失、可回看。

## What I already know

- Android 端已有 5 Tab：首页 / 流程 / 预算 / 指南 / 我的（`ui/nav/AppRoot.kt`）。
- 「我的」页已有自由笔记功能（`note` 表：id/title/body/created_at/updated_at），无分类、无类型、无阶段关联；入口深（我的 Tab → 笔记区 → 点 +）。
- 已有 Room 基础设施：`NoteDao`、`NoteContactRepository`、`MoreViewModel`；DB 版本 v1（`AppDatabase.kt`），schema 已导出（1.json）。
- 已有 `StageEntity`（14 阶段模板）与 `stage_override` 表。
- 导入导出：`ImportExportRepository` 处理 JSON 备份/恢复，`note` 已包含在备份中。
- spec：`.trellis/spec/android/data-layer.md`（Room 约定）、`type-money.md`（金额 INTEGER cents）。

## Assumptions (temporary)

- 新功能为独立实体（随手记 ≠ 现有自由笔记），避免污染现有 note 表语义。
- 快速记录 = 打开 App 后 2 次点击内可输入文字保存。
- 分类整理 = 按记录类型（购物愿望 / 阶段备忘）分组，购物愿望可带品类标签（家具/家电/生活用品），备忘可关联 14 阶段。

## Open Questions

- 分类维度的确认（类型 + 品类 + 阶段）
- 是否需要勾选完成（购物愿望买完可打勾归档）？

## Decisions (ADR-lite)

### D2 · 快速入口仅放「我的」页

**Context**：候选方案有首页常驻输入框、全局 FAB、我的页入口。
**Decision**：在「我的」页新增随手记区块（+ 按钮新增），与现有笔记区块并列。
**Consequences**：
- (+) 改动最小，与现有信息架构一致。
- (-) 入口较深（需切到我的 Tab），"随时记"的摩擦略高；后续可在首页补入口作为迭代。

## Decisions (ADR-lite)

### D1 · 随手记为独立新实体

**Context**：现有「我的」页有自由笔记（note 表：title+body），随手记需要分类、阶段关联、完成状态等结构化字段。
**Decision**：新建 `quick_note` 表，现有自由笔记保持不动。
**Consequences**：
- (+) 语义清晰，互不干扰；备份格式向后兼容（新增字段，旧版本可忽略）。
- (-) 「我的」页会出现两个记录类区块，需要信息架构上区分清楚。

### D3 · 分类维度：类型 + 品类/阶段

**Context**：候选方案有自由标签体系、仅类型二分、类型+品类/阶段。
**Decision**：每条随手记二选一类型：购物愿望（再选品类：家具/家电/生活用品）或阶段备忘（再关联 14 阶段之一）。整理视图按类型分两大组，购物愿望按品类分组，备忘按阶段分组。
**Consequences**：
- (+) 结构化程度高，整理视图直观；品类/阶段为固定枚举，无需用户维护。
- (-) 灵活性低于自由标签；品类枚举后续扩展需改代码（可接受，装修场景品类稳定）。

### D4 · 支持勾选完成

**Context**：购物愿望买完、备忘处理完后的归宿——删除还是标记完成。
**Decision**：每条随手记带 `is_done` 字段，可勾选完成；整理视图中已完成项折叠/置底展示，不自动删除。
**Consequences**：
- (+) 保留"买过什么/处理过什么"的记录价值。
- (-) 列表多一个状态维度，UI 需处理已完成项的视觉弱化。

## Requirements (evolving)

### MVP（标准包，用户已确认）

- **数据模型**：新建 `quick_note` 表（id / content / type: wish|memo / category: furniture|appliance|daily（仅 wish）/ stage_id（仅 memo）/ is_done / created_at / updated_at）。
- **入口**：「我的」页新增随手记区块，+ 按钮新增；每条可编辑、删除、勾选完成。
- **整理视图**：随手记区块内按类型分两大组——购物愿望按品类（家具/家电/生活用品）分组，阶段备忘按 14 阶段分组；已完成项折叠/置底。
- **备份恢复**：JSON 导出包含 quick_note；导入兼容（旧备份无 quick_note 字段时跳过）。
- **数据重置**：清除全部数据时包含 quick_note。
- **DB 迁移**：Room v1 → v2 Migration，schema 导出 2.json。

## Acceptance Criteria (evolving)

- [ ] 「我的」页可见随手记区块，可新增购物愿望（选品类）与阶段备忘（选阶段）。
- [ ] 每条随手记可编辑、删除、勾选完成；已完成项在整理视图中折叠/置底。
- [ ] 整理视图按类型分组：购物愿望按品类分组、阶段备忘按阶段分组，分组标题直观。
- [ ] JSON 备份包含随手记；用旧版本备份（无随手记字段）导入不报错。
- [ ] 清除全部数据后随手记为空。
- [ ] Room Migration v1→v2 不丢数据；DAO 单测覆盖分组查询；ViewModel 单测通过。
- [ ] lint / unit test 全绿。

## Definition of Done (team quality bar)

- Room DAO 单测覆盖新增聚合查询
- ViewModel 单测覆盖
- lint / unit test 通过
- JSON 备份/恢复兼容新数据

## Out of Scope (explicit)

- 首页快速入口 / 全局 FAB（后续迭代）。
- 首页行动中心露出备忘提醒。
- 购物愿望金额与预算联动。
- 自由标签体系。
- 提醒 / 通知。

## Technical Notes

- 勘察文件：`NoteContactEntities.kt`、`NoteContactDaos.kt`、`MoreScreen.kt`、`AppRoot.kt`
- DB v1 → v2 需 Migration
- 现有模式参考：`NoteDao`（upsert/delete/observeAll/exportAll/clearAll）、`MoreViewModel`、`MoreScreen.kt` 的 SectionCard + Dialog 模式
- 品类枚举（家具/家电/生活用品）与类型枚举（wish/memo）建议存 TEXT 常量，与现有 `stage_override` 等表风格一致
