# 随手记展示优化

## Goal

随手记（购物愿望 / 阶段备忘）目前每条独占一张 SectionCard，垂直空间占用大、视觉密度低、显示效果差。目标是压缩展示空间、提升信息密度，同时保留勾选完成 / 编辑 / 删除能力。

## What I already know

* 展示位置：`MoreScreen.kt`（"我的" tab）单个 LazyColumn 内，随手记 section 位于 196–277 行
* 单条卡片 `QuickNoteCard`（MoreScreen.kt:642–675）：Checkbox + 全文 content（bodyLarge）+ groupLabel 副标题（bodySmall）+ Edit IconButton + Delete IconButton
* groupLabel 副标题与所在分组标题（购物愿望→家具/家电/生活用品；阶段备忘→阶段名）完全重复，属于冗余信息
* 数据模型 `QuickNoteEntity`：id / content / type(wish|memo) / category / stageId / isDone / createdAt / updatedAt（日期为 yyyy-MM-dd 字符串，全库约定）；UI 目前未展示任何时间戳
* 排序由 DAO 决定：`ORDER BY is_done ASC, updated_at DESC`（未完成在前，已完成沉底）
* 分组逻辑：wish 按 category 分组、memo 按 stageId 分组，组标题已单独渲染
* 空状态占位（205–214 行）是此前 UX 审计认可的亮点，需保留
* spec 约束（.trellis/spec/android/ui-theme.md 交互反馈约定）：删除必须 ConfirmDeleteDialog 确认、写操作需 Snackbar 反馈、对话框需 imePadding、表单校验不静默失败

## Assumptions (temporary)

* 不改数据模型与 DAO，仅改 UI 展示层
* 新增/编辑弹窗 QuickNoteDialog 本身不在本次范围内

## Open Questions

（已全部解决）

## Requirements

* 展示形态：同组笔记合并到一张 SectionCard 内，每条为紧凑单行（不再每条一张卡片）
* 单条结构：Checkbox + 单行文本（maxLines = 1, ellipsis），去掉与组标题重复的 groupLabel 副标题
* 交互：点击行内容 → 编辑弹窗；长按行 → 删除确认（ConfirmDeleteDialog）；行尾不再放编辑/删除图标
* 保留：勾选完成（已完成项弱化 + 删除线 + 排序沉底）、空状态占位、删除确认、Snackbar 反馈

## Acceptance Criteria

* [ ] 同组笔记合并在一张卡片内，单条高度明显低于现状
* [ ] 不再显示与组标题重复的副标题
* [ ] 点击行 → 打开编辑弹窗；长按行 → 弹删除确认
* [ ] 超长内容单行截断显示省略号
* [ ] 勾选完成 / 删除确认 / 空状态均正常工作，符合 ui-theme.md 交互反馈约定

## Definition of Done (team quality bar)

* Lint / typecheck 通过
* 相关既有测试（QuickNoteDaoTest / MoreViewModelTest）不回归
* UI 行为符合 .trellis/spec/android/ui-theme.md 交互反馈约定

## Out of Scope (explicit)

* 数据模型 / DAO / Repository 改动
* QuickNoteDialog 新增/编辑弹窗改版
* 随手记独立页面（仍留在"我的" tab 内）
* 时间戳展示（模型里有但本次不上 UI）

## Technical Approach

改造 `MoreScreen.kt` 内随手记展示区（196–277 行）与 `QuickNoteCard`（642–675 行）：

1. 分组结构改为「组标题 Text + 单张 SectionCard」，卡片内部用 `Column` 顺序渲染该组所有笔记行，行间可用细分隔线（HorizontalDivider）或紧凑 padding
2. `QuickNoteCard` 重构为 `QuickNoteRow`：Checkbox（尺寸可缩小）+ 单行文本（`maxLines = 1`、`overflow = Ellipsis`、`bodyMedium`），整行 `combinedClickable(onClick = onEdit, onLongClick = onDelete)`；删除仍走 `deleteQuickNoteId` → `ConfirmDeleteDialog` 链路
3. 已完成态：文本删除线 + 整行 alpha(0.5f)，沿用现状
4. `groupLabel` 参数删除（信息已由组标题承载）

## Decision (ADR-lite)

**Context**: 每条一张卡片的展示占空间大、含冗余副标题，需要更高信息密度。
**Decision**: 采用「同组合并一卡 + 紧凑单行」形态；编辑=点击、删除=长按（用户选定）；长文本单行截断（用户选定）。
**Consequences**: 长按删除可发现性略低（换来最紧凑的行密度）；全文需进编辑弹窗查看；后续如需批量管理可在此基础上扩展滑动/多选。

## Technical Notes

* 关键文件：android/app/src/main/java/com/renovation/guardian/ui/more/MoreScreen.kt（196–277 展示区、642–675 QuickNoteCard）
* 共享组件：ui/components/Components.kt（SectionCard / SectionTitle / EmptyState）
* 交互反馈约定：.trellis/spec/android/ui-theme.md
