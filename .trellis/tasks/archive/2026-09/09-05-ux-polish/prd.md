# 优化 App 使用体验

## Goal

用户在真机实际使用装修管家 Android 版后提出「优化软件使用体验」。范围宽泛，需要通过 brainstorm 收敛出具体、可验证的体验改进清单，形成 MVP。

## What I already know

- Android 版为五 Tab 结构：首页 / 流程 / 预算 / 指南 / 我的（`ui/nav/AppRoot.kt`）。
- 已知体验摩擦（来自近期会话/任务）：
  - 流程页种子数据曾为空（已修复，启动 1-2 秒后 Flow 刷新）——首次启动可能闪空列表。
  - AGP/工具链很新（AGP 9.3.2 / Gradle 9.5 / Kotlin 2.3.20 / compileSdk 37 / targetSdk 35，edge-to-edge 强制）。
  - 随手记刚上线（我的页区块）；首页快速入口 / 全局 FAB 在 quick-notes 的 Out of Scope，留作迭代。
- 手测清单（spec）：首次设置 → 首页打卡 → 流程勾选 → 预算录入 → CSV 导出 → JSON 导入/导出 → 数据重置。

## Assumptions (temporary)

- 以真机（hanhu 的手机）实际体验为准；用户能描述具体不爽点。

## Open Questions

- （已收敛）MVP = Top 6；Top 7-10 留后续。

## Decisions (ADR-lite)

### D1 · MVP 范围 = Top 6（全部 S 级）

**Context**：体检 40+ 条发现，Top 10 按感知度×严重度÷工作量排序。
**Decision**：本任务修 Top 1-6；Top 7-10（热区 / rememberSaveable / 空态引导 / 预算重算）留后续迭代。
**Consequences**：
- (+) 单会话可完成，全部是用户可即时感知的修复。
- (-) Top 7-10 延后（记录在 Out of Scope，不丢失）。

## Requirements (MVP = Top 6，详见 research/ux-audit.md)

1. **笔记可删除**：MoreScreen 笔记卡片补删除入口（当前 deleteNote 是死代码）。
2. **保存校验与反馈**：BudgetScreen 必填校验（支出金额/分类名），非法输入给错误提示而非静默失败。
3. **删除确认**：5 处一键直删（支出/联系人/随手记/空间/自定义任务）补确认对话框。
4. **首启体验**：流程页种子写入完成前给加载占位（不再白屏）；首页在无任何任务数据时不误报「全部完成 🎉」。
5. **键盘遮挡**：全工程对话框/输入区补 `imePadding`（重点：流程页添加任务、随手记/笔记对话框、支出对话框）。
6. **保存/导出反馈**：接通已就位的 SnackbarHost——保存成功、导出完成、导入结果给 Snackbar 提示。

## Acceptance Criteria

- [ ] 「我的」页笔记可删除（有确认）。
- [ ] 预算页非法输入（空金额/无分类）保存时给出可见错误提示。
- [ ] 5 处删除操作均弹确认框；确认后删除。
- [ ] 首次启动流程页不再白屏（加载占位）；首页不再误报完成。
- [ ] 键盘弹出时输入框不被遮挡（真机验证流程页添加任务）。
- [ ] 保存/导出/导入操作有 Snackbar 反馈。
- [ ] lint / unit test 全绿。

## Out of Scope（Top 7-10，后续迭代）

- 任务行点击热区扩大（HomeScreen）。
- rememberSaveable 保持 Tab 状态。
- 预算空分类引导 + FAB 遮挡微调。
- 编辑房屋信息后预算重算流程（对齐 Web 版）。
- strings.xml 抽取与 EmptyState 组件复用（低优先级，可顺带）。

## Technical Notes

- 体检报告：`research/ux-audit.md`（40+ 条，含文件:行号与建议修法）。
- 关键落点：`MoreScreen.kt:151`（笔记删除）、`BudgetScreen.kt:219,252`（校验）、`StagesScreen.kt:46,150`（白屏/imePadding）、`HomeScreen.kt`（完成误报）。
- SnackbarHost 已就位只差调用（BudgetScreen.kt:91）。

## Technical Notes

- 勘察入口：`ui/nav/AppRoot.kt`、各 Tab Screen/VM、`ui/theme/`。
