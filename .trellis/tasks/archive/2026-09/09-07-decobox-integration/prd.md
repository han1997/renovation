# 集成 decobox（装修宝典）预算计算器与需求规划到 Android App

## Goal

把 https://www.decobox.online/ 的核心能力集成到 Android App（装修管家）：**逐空间预算计算器**（整装/半包/局改三模式）与**需求规划**（功能目录→分配空间→清单导出），作为预算 Tab 的新子入口。数据离线内置，延续 App 无后端、离线优先的架构。

## What I already know

* decobox.online 是 React SPA,功能包括:预算计算器(三模式四步向导)、实时预算对比、需求规划(150 类型 / 819 需求项)、清单导出(文本+长图)、案例画廊(本次不做)
* 网站 JS bundle 已下载：`C:\Users\hanhu\.local\share\opencode\tool-output\tool_07a7dfc56001Xi9j3Jc51jlZ7f`（~783KB），数据结构已完整提取（见 research/decobox-data-model.md）
* 本仓库与该网站无代码关系；Android App：Kotlin 2.3.20 + Compose BOM 2026.08.00 + Room 2.8.4（schema v2），minSdk 24，离线优先
* 用户决策（已确认）：
  - 范围 = 预算计算器 + 需求规划（不含案例/联系）
  - 入口 = 预算 Tab 新增子入口（计算器）；需求规划放"我的"Tab（替代旧空间需求位置）
  - 数据 = 从网站 bundle 提取整理，内置离线 JSON
  - 旧"我的→空间需求"数据**直接弃用不迁移**，需求规划全面替代
  - 计算器结果**一键写入预算**：按大项覆盖 `budget_category.planned_cents`，总预算同步，支出不动
  - **本项目永久禁止使用子代理**，全部工作主会话完成

## Requirements

### R1 数据层
* 新增 `assets/decobox_catalog.json`：材料目录（墙面/顶面/地面/其他主材/空间附加项/全屋工程/局改项）+ 计算参数（管理费率、面积估算系数、默认价格）
* 新增 `assets/decobox_requirements.json`:10 预设空间 + 150 需求类型 + 819 需求项
* KnowledgeCache 扩展加载两个新 JSON（沿用现有模式）

### R2 计算引擎（纯 Kotlin）
* `QuoteCalculator`：整装/半包/局改三模式
* 面积估算：墙面 = 4×√(地面)×层高；吊顶三方案（不吊顶/局部/全部）
* 工艺联动：乳胶漆基层 7 步、瓷砖铺贴 2 步、吊顶 fixedCrafts
* 其他主材计价：门（门洞尺寸→下单面积、超规加价）、窗台石（左右耳+磨边）、风暖/坐便品牌矩阵、多选洁具
* 采购方式：自购不计入总价单独列出；整装管理费 8%×(主材+人工辅材)
* 局改：墙面刷新两档、地面更换（套用地面目录）、拆旧/成品保护/垃圾清运
* 输出：分空间分部位明细行（含 sourcing）+ 汇总（人工辅材/代购主材/自购/管理费/总价/预算对比）

### R3 Room v3
* 新表 `quote_plan`（id/name/mode/state_json/created_at/updated_at），方案状态存 JSON blob
* **删除** `space_need` / `space_need_stage` 两表（数据弃用）
* 显式 Migration 2→3；MigrationTest 扩展
* SpaceNeedRepository 及相关 UI/备份逻辑移除

### R4 计算器 UI
* 预算 Tab 顶部"逐空间报价"入口卡
* 四步向导：房屋信息 → 划分空间（含按面积推荐）→ 逐空间选材 → 清单
* 实时预算条（总价 vs 预算、超支警示）
* 方案保存/加载/删除（多方案并存）
* 清单页：复制文本 + 导出长图 + 一键写入预算

### R5 清单导出
* 文本格式对齐网站（分空间分组、自购标注、预算对比行）
* 长图：Canvas + StaticLayout 手绘渲染器（research 结论方案 B）
* 保存：MediaStore（API 29+）；分享：FileProvider + ACTION_SEND（Manifest 新增 provider）

### R6 需求规划
* "我的"Tab 新入口（替换旧空间需求区块）
* 四步：选需求（分类+搜索）→ 添加空间（预设+自定义）→ 分配需求（重要度 4 档）→ 生成清单
* 清单导出：文本 + 长图
* 状态持久化（Room，同 quote_plan 模式或独立表）

### R7 预算打通
* 清单页"写入预算"：按大项（人工辅材/主材/管理费等）覆盖对应 `budget_category.planned_cents`，`house_profile.total_budget_cents` 同步为清单总价；已有支出不动
* 写入前确认对话框

## Acceptance Criteria

* [ ] 三种模式均可完成四步流程并生成清单，金额与网站逻辑一致（抽样对照）
* [ ] 实时预算随选材联动更新，超预算有警示
* [ ] 清单可复制为文本、可导出长图分享/保存
* [ ] 计算器方案保存后重进 App 数据不丢（Room 持久化）
* [ ] 需求规划可把需求分配到空间、标重要度并导出清单
* [ ] 一键写入预算后分类计划金额与总预算正确更新，支出不受影响
* [ ] 旧 space_need 表数据在升级后不再出现（UI 无旧入口）
* [ ] 全程离线可用
* [ ] MigrationTest 2→3 通过；单测/lint/assembleDebug 全绿

## Definition of Done

* 单元测试覆盖计算引擎（三模式汇总、面积估算、管理费、自购剔除、局改）
* `gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 通过
* 遵循 .trellis/spec/android/ 规范（金额 cents、分层边界、中文注释）
* README 功能概览更新

## Out of Scope

* 过往案例、联系我们（微信/抖音引流）
* Web 版任何改动
* iOS / 云同步 / 后端服务
* 网站数据自动更新（静态快照，标注 2025–2026 行情）
* 计算器方案/需求规划数据进 JSON 备份（第一版）

## Technical Approach

* 数据：Python 提取脚本（tools/）→ assets JSON → kotlinx-serialization 模型 → KnowledgeCache
* 计算：`ui/quote/engine/QuoteCalculator.kt` 纯函数，输入 = 目录 + 方案状态，输出 = QuoteResult
* 持久化：方案状态 JSON blob 存 Room（结构演进不受 schema 约束）
* UI：`ui/quote/`（计算器）+ `ui/planner/`（需求规划），Navigation Compose 新增路由
* 导出：`ui/quote/export/QuoteImageRenderer.kt`（Canvas+StaticLayout）+ FileProvider

## Decision (ADR-lite)

**Context**: 网站功能如何落到 Android；旧空间需求如何处置
**Decision**: 计算器+需求规划全量移植（不含案例）；方案状态用 JSON blob 而非规范化表；旧 space_need 直接删除；长图用 Canvas+StaticLayout 而非 Compose GraphicsLayer
**Consequences**: blob 方案牺牲可查询性换取结构灵活性（第一版无跨方案查询需求）；删表需 MigrationTest 防回归；Canvas 渲染器一次性代码 ~300 行但全版本兼容可单测

## Research References

* research/decobox-data-model.md — 网站数据模型与计算公式全解
* research/decobox-materials-sample.md — 材料库样本（含价格）
* research/decobox-requirements-catalog.md — 需求规划目录结构
* research/android-long-image-export.md — 长图导出方案对比（推荐 Canvas+StaticLayout）
