# Data Layer

## Overview

数据分两类：

- **只读知识数据**：`assets/knowledge.json` / `prices.json` / `decobox_catalog.json` / `decobox_requirements.json`，启动时一次性读入内存缓存；
- **用户数据**：Room（SQLite）16 张表，由 DAO / Repository 读写。

## Room 表(v3)

`AppDatabase`(`data/db/AppDatabase.kt`)`version = 3`,`exportSchema = true`(schema 落 `app/schemas`)。v1 起全部走显式 `Migration`,`fallbackToDestructiveMigration()` 仅兜底未定义版本跳变:

- `MIGRATION_1_2`:新增 `quick_note`(随手记);
- `MIGRATION_2_3`:新增 `quote_plan`(逐空间报价,`state_json` 存完整向导状态)与 `planner_state`(需求规划,单行),**删除**已弃用的 `space_need` / `space_need_stage`(数据不迁移)。schema 1.json / 2.json / 3.json 均已导出。

迁移测试不走 `MigrationTestHelper`(AGP9 无 mergeDebugUnitTestAssets),手工建旧版库跑迁移,覆盖 v1→v3 与 v2→v3,见 `MigrationTest.kt`。

| 表 | 实体 | 说明 |
|----|------|------|
| `house_profile` | `HouseProfileEntity` | 房屋信息，单行（`id` 固定 `1`） |
| `stage` | `StageEntity` | 14 阶段目录（种子） |
| `task_template` | `TaskTemplateEntity` | 初始模板及用户新增/修改的阶段任务（可变） |
| `task_completion` | `TaskCompletionEntity` | 模板任务勾选 / 日期 |
| `task` | `TaskEntity` | 用户任务（自建 `CUSTOM` / 派生 `DERIVED_FROM_SPACE`） |
| `stage_override` | `StageOverrideEntity` | 整段完成覆盖 |
| `budget_category` | `BudgetCategoryEntity` | 预算分类 |
| `expense` | `ExpenseEntity` | 支出条目 |
| `quote_plan` | `QuotePlanEntity` | decobox 逐空间报价方案(`mode`: full/semi/partial;`state_json` 存完整向导状态) |
| `planner_state` | `PlannerStateEntity` | 需求规划状态(单行,`id` 固定 1;`state_json`) |
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
- Android 备份 v2 使用整数分；v1 元金额仅作读取兼容。完整契约见 [全 App 数据与交互契约](./app-polish-contracts.md)。

## 知识数据 assets

- `assets/knowledge.json`：镜像 Web `window.DATA`（`version` + `stages` / `checklists` / `tips` / `styles` / `styleQuiz` / `materialTimeline` / `modes` / `whoBuilds` / `glossary` / `spaceNeeds` 等）。结构见 `data/knowledge/KnowledgeJson.kt`。
- `assets/prices.json`：镜像 Web `window.PRICES`（`version` + `reserveRatio` / `tiers` / `grades` / `rates` / `reference`）。结构见 `data/knowledge/PricesJson.kt`。
- `assets/decobox_catalog.json`:decobox.online v1.4.0 材料目录与计算参数(墙/顶/地/其他主材/附加项/全屋工程/局改/门·洁具品牌矩阵/面积推荐)。由 `tools/extract_decobox.py` 从站点 bundle 生成;结构见 `data/knowledge/DecoboxCatalogJson.kt`。
- `assets/decobox_requirements.json`:需求规划目录(10 预设空间 / 150 需求类型 / 819 叶子项)。同脚本生成;结构见 `data/knowledge/DecoboxRequirementsJson.kt`。

- **Android 知识数据独立维护**；Web 版自 2026-09-05 起已封存，后续知识更新仅修改 Android 的 assets 与相关模型，不再要求同步 Web 数据或检查双端文案一致性。
> **Warning(数值契约)**:资产 JSON 的整数值(单价/费率/默认数量等)必须以整数输出,禁止带浮点尾数(如 `1000.0`)。kotlinx-serialization 严格模式读 Int 字段遇 `1000.0` 会抛 `JsonDecodingException`。提取脚本 `tools/extract_decobox.py` 的 `to_jsonable()` 已把整值 float 归一为 int;手工改 assets 时留意。
- `reference.items[].price` 是自由文本（如 "30–80元/㎡"），非数字，Android 端仅展示或做区间解析，不参与计算。

## 启动装载与种子

1. `RenovationApp` 持有 `KnowledgeCache`，`onCreate` 中调用 `load()` 一次性解析两个 assets。
2. `KnowledgeSeeder` 在事务中初始化阶段、初始任务与验收目录。阶段/验收目录只读，任务模板可由用户增删改。
3. `AppContainer.clearAllData()` 清除用户数据并恢复默认任务，保留阶段/验收目录；不删除外部文件。已初始化的空任务表不自动重新播种。

> **Warning（回归防护）**：`seedIfEmpty` **必须在 `RenovationApp.onCreate` 启动链路被调用**（应用级 `CoroutineScope(SupervisorJob() + Dispatchers.Default)` 异步执行，不用 GlobalScope）。该函数静默幂等（种子表 count>0 即跳过），但若无人调用，`stage` / `task_template` / `checklist` 三张种子表永远为空，流程页完全空白且**无任何报错**——移植 / 重构启动链路时极易遗漏（曾实际发生，见任务 `09-05-fix-stage-seed`）。
>
> **检查点**：`grep seedIfEmpty` 全工程应 ≥2 处命中——定义处 + `RenovationApp.onCreate` 启动调用；少于 2 处即回归。单测防护见 `app/src/test/.../data/knowledge/KnowledgeSeederTest.kt`（真实 assets + in-memory Room 验证种子非空与幂等）。

## 导入 / 导出

- `exportJson()`：Room v3 完整一致性快照，备份格式 v2，金额为分；包含可变任务模板、完成/日期、报价和规划。
- `prepareImport()`：识别 v1/v2 并校验，供 UI 预览确认；`restore()`：单事务替换，失败回滚。
- v1 缺失模块保留，未知版本/空对象/非法引用拒绝；细则见 [全 App 契约](./app-polish-contracts.md)。
- CSV 仍走 `ui/budget/Csv.kt`，SAF IO 由 `ui/nav/ExportActions.kt` 负责。
