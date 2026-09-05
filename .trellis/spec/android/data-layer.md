# Data Layer

## Overview

数据分两类：

- **只读知识数据**：`assets/knowledge.json` + `assets/prices.json`，启动时一次性读入内存缓存；
- **用户数据**：Room（SQLite）16 张表，由 DAO / Repository 读写。

## Room 表（v2）

`AppDatabase`（`data/db/AppDatabase.kt`）`version = 2`，`exportSchema = true`（schema 落 `app/schemas`），`fallbackToDestructiveMigration()` 已移除，v1→v2 起全部走显式 `Migration`（`MIGRATION_1_2` 新增 `quick_note` 表；schema 1.json / 2.json 均已导出）。

| 表 | 实体 | 说明 |
|----|------|------|
| `house_profile` | `HouseProfileEntity` | 房屋信息，单行（`id` 固定 `1`） |
| `stage` | `StageEntity` | 14 阶段目录（种子） |
| `task_template` | `TaskTemplateEntity` | 内置模板任务（种子） |
| `task_completion` | `TaskCompletionEntity` | 模板任务勾选 / 日期 |
| `task` | `TaskEntity` | 用户任务（自建 `CUSTOM` / 派生 `DERIVED_FROM_SPACE`） |
| `stage_override` | `StageOverrideEntity` | 整段完成覆盖 |
| `budget_category` | `BudgetCategoryEntity` | 预算分类 |
| `expense` | `ExpenseEntity` | 支出条目 |
| `space_need` | `SpaceNeedEntity` | 空间需求 |
| `space_need_stage` | `SpaceNeedStageEntity` | 空间 ↔ 阶段关联 |
| `checklist` | `ChecklistEntity` | 验收清单目录 |
| `checklist_item` | `ChecklistItemEntity` | 验收条目目录 |
| `checklist_item_check` | `ChecklistItemCheckEntity` | 验收条目勾选 |
| `note` | `NoteEntity` | 笔记 |
| `contact` | `ContactEntity` | 联系人 |
| `quick_note` | `QuickNoteEntity` | 随手记（type: wish 带 category / memo 带 stage_id，is_done 可勾选） |

## 关键聚合（DAO）

`data/db/*Dao.kt` 提供 4 个关键聚合，均有单元测试覆盖（`src/test/.../data/db/AggregatesTest.kt`）：

1. **首页三段分组**：`TaskDao.observeBetween(from, to)` —— 按 `due_date` 区间查询（逾期 / 今天 / 未来 7 天）。
2. **阶段完成度**：`TaskDao.observeAllProgress()` —— 每阶段模板任务总数 / 已完成数 / 百分比。
3. **分类超支**：`BudgetCategoryDao.observeWithSpent()` —— `planned_cents` 与 `SUM(expense.amount_cents)` 的差。
4. **CSV 扁平视图**：`ExpenseDao.observeForCsv()` —— `expense` LEFT JOIN `budget_category` 得 `ExpenseExportRow`。

## 金额约定（INTERGER cents）

所有金额字段一律用 **`Long`（分，cents）**，避免浮点累计误差：

- `budget_category.planned_cents`、`expense.amount_cents`、`house_profile.total_budget_cents`。
- 换算统一走 `util/MoneyUtil.kt`（`fromYuan` / `toYuan` / `format` / `formatFull`）。
- Repository 层对外接口可用元（`Double`），内部写入前必须 `MoneyUtil.fromYuan` 折算。
- JSON 备份沿用 Web 语义用元（float），导入用 `MoneyUtil.fromYuan` 写 cents（见 `ImportExportRepository`）。

## 知识数据 assets

- `assets/knowledge.json`：镜像 Web `window.DATA`（`version` + `stages` / `checklists` / `tips` / `styles` / `styleQuiz` / `materialTimeline` / `modes` / `whoBuilds` / `glossary` / `spaceNeeds` 等）。结构见 `data/knowledge/KnowledgeJson.kt`。
- `assets/prices.json`：镜像 Web `window.PRICES`（`version` + `reserveRatio` / `tiers` / `grades` / `rates` / `reference`）。结构见 `data/knowledge/PricesJson.kt`。
- **Android 知识数据独立维护**；Web 版自 2026-09-05 起已封存，后续知识更新仅修改 Android 的 assets 与相关模型，不再要求同步 Web 数据或检查双端文案一致性。
- `reference.items[].price` 是自由文本（如 "30–80元/㎡"），非数字，Android 端仅展示或做区间解析，不参与计算。

## 启动装载与种子

1. `RenovationApp` 持有 `KnowledgeCache`，`onCreate` 中调用 `load()` 一次性解析两个 assets。
2. `KnowledgeSeeder`（`data/knowledge/KnowledgeSeeder.kt`）负责把「目录数据」（阶段 / 模板任务 / 验收清单）写入 Room，属只读种子，不清除。
3. 用户数据不清除种子；`AppContainer.clearAllData()` 只清除用户表，保留只读种子表。

> **Warning（回归防护）**：`seedIfEmpty` **必须在 `RenovationApp.onCreate` 启动链路被调用**（应用级 `CoroutineScope(SupervisorJob() + Dispatchers.Default)` 异步执行，不用 GlobalScope）。该函数静默幂等（种子表 count>0 即跳过），但若无人调用，`stage` / `task_template` / `checklist` 三张种子表永远为空，流程页完全空白且**无任何报错**——移植 / 重构启动链路时极易遗漏（曾实际发生，见任务 `09-05-fix-stage-seed`）。
>
> **检查点**：`grep seedIfEmpty` 全工程应 ≥2 处命中——定义处 + `RenovationApp.onCreate` 启动调用；少于 2 处即回归。单测防护见 `app/src/test/.../data/knowledge/KnowledgeSeederTest.kt`（真实 assets + in-memory Room 验证种子非空与幂等）。

## 导入 / 导出

- `ImportExportRepository`（`data/repo/ImportExportRepository.kt`）：
  - `exportJson()` 导出为与 Web `Store.state` 对齐的 JSON（`profile` / `tasksDone` / `taskDates` / `customTasks` / `stageOverride` / `budgetCategories` / `expenses` / `checks` / `notes` / `contacts` / `spaces`）。
  - `importJson()` 在**单个 `withTransaction`** 内先清空用户表再重插，保证原子性；失败返回 `ImportResult(success=false, error=...)`。
- CSV 导出：`ui/budget/Csv.kt` 的纯函数 `buildCsv(rows)` 拼 CSV（含表头、金额换算为元、字段转义）；落盘走平台 SAF（`CreateDocument`），不在本层。