# 空间需求规划：个性化空间清单与流程联动

## Goal

为装修管家 app 新增"空间需求规划"能力，让用户在装修早期（首次设置向导或独立入口）系统化地表达个性化空间需求（如衣帽间、大横厅、书房+电竞房、中西双厨等），并将这些需求自动关联到装修流程（派生任务到对应阶段）和预算（影响相关分类），解决目前"需求只能散落笔记、无法应用到流程"的缺口。

## What I already know（仓库现状调研）

### 现有 app 对个性化需求的支持（缺口）
- **首次设置向导**（`app.js:111-248`）：3 步（面积/城市/日期 → 装修方式 → 档次+预算），纯参数化，**不收集空间需求**
- **我的页 - 笔记**（`more.js:33-46`）：自由文本 `{id, title, text, updatedAt}`，能记但不成结构，无法关联流程/预算
- **流程页 - 自定义任务**（`stages.js`，`Store.state.customTasks`）：`[{id, stageId, text, date}]`，是任务层补充，不是需求层
- **预算页**（`budget.js`）：分类预算 `budget.categories`，无"空间/房间"维度

### 数据结构（`storage.js:5-20` defaults）
```
profile: {area, tier, mode, grade, styleId, startDate, totalBudget, createdAt}
customTasks: [{id, stageId, text, date}]
notes: [{id, title, text, updatedAt}]
budget: {categories: [], expenses: []}
```
- `Store.state` 无 `spaces` / `spaceNeeds` 字段 → 需新增
- `load()` 有嵌套结构修复逻辑（`storage.js:30-39`）→ 新增字段需在此加修复

### 流程阶段（`DATA.stages`，14 阶段）
- 14 阶段固定，已有 `tasks`/`warnings`/`buy`/`acceptIds`
- 空间需求需派生任务到对应阶段，例如：
  - 衣帽间 → `s-custom`（定制柜阶段）的定制柜量尺任务
  - 大横厅 → `s-demolition`（拆改阶段）的拆墙任务
  - 电竞房 → `s-hydropower`（水电阶段）的电路改造任务 + `s-custom` 的电脑桌定制

### 知识库（`knowledge.js`）
- 有 `DATA.styles`/`DATA.modes`/`DATA.glossary` 等，但**无空间需求清单知识**
- 需新增 `DATA.spaceNeeds`（常见空间需求清单 + 关联阶段 + 预算影响提示）

## Assumptions（待验证）

- 空间需求清单入口：向导里加一步 / 独立入口 / 两者都有 ← 见下
- 需求与流程的联动方式：自动派生任务 vs 仅展示提示 ← 见下
- 需求与预算的联动：自动追加预算项 vs 仅提示影响金额 ← 见下

## Open Questions

- 需求变更后已派生任务的同步策略（已有合理默认，待最终确认一并说明）

## Requirements（evolving）

1. 新增"空间需求"数据结构（`Store.state.spaces`）
2. **入口**：向导内快速勾选（第 2 步，装修方式之后）+ 我的页完整管理，共享同一份数据
3. **流程联动**：勾选空间需求 → 自动在关联阶段派生任务（复用 `customTasks` 机制，用 `spaceId` 标记来源）
4. **预算联动**：勾选空间需求 → 在预算相关分类展示"含XX约+X元"提示（仅提示，不自动改预算金额）
5. **需求清单来源**：`DATA.spaceNeeds` 预置 15-20 种常见需求（带关联阶段+预算影响），用户可自定义任意需求（自定义项需手填关联阶段）
6. 新增 `DATA.spaceNeeds` 常见空间需求知识库
7. **变更同步策略**：派生任务用 `spaceId` 标记来源；删需求时连带删除其派生任务（已打卡的保留为普通自定义任务，仅清除 spaceId 标记）

## Acceptance Criteria（evolving）

- [ ] 向导第 2 步可勾选常见空间需求（至少 15 种预设）
- [ ] 我的页有"空间需求"卡片，可查看/增删/编辑需求
- [ ] 勾选预设需求 → 自动在关联阶段派生对应任务（至少 3 种需求正确联动：衣帽间→定制柜、大横厅→拆改、电竞房→水电）
- [ ] 预算相关分类显示"含XX约+X元"提示
- [ ] 删除需求 → 连带删除其派生任务（已打卡的保留为普通任务）
- [ ] 自定义需求可手动指定关联阶段
- [ ] 数据随 JSON 导入导出备份（`spaces` 字段）
- [ ] `node --check` 通过所有 JS

## Definition of Done

- 零依赖原则保持（vanilla JS，无 npm）
- IIFE 全局模式一致（`window.DATA`/`Store`/`Views`）
- 中文化（UI、注释、commit）
- `node --check` 通过所有 JS

## Out of Scope（explicit）

- 3D 户型图 / 空间可视化
- 基于户型的自动布局推荐
- AI 根据需求生成设计方案
- 与设计师协同 / 分享需求清单

## Technical Notes

- 插入点：`app.js` 向导 `showWizard()` / `storage.js` defaults / `knowledge.js` DATA / 可能新增 `js/views/spaces.js`
- 向导改为 4 步会动到 `wz-step` 计数与 `renderStep()` 分支，需谨慎
- 空间需求派生任务可复用 `customTasks` 机制（已有 id/stageId/text 结构），或新增独立 `spaceTasks`
