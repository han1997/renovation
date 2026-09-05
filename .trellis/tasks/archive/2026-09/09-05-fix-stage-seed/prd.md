# 修复流程页空白：种子数据未写入（seedIfEmpty 无调用方）

## Goal

用户在真机运行 App，「流程」页完全空白。根因已实锤：`KnowledgeSeeder.seedIfEmpty()` 定义了但全工程无任何调用方，`stage` / `task_template` / `checklist` 三张种子表永远为空，流程页（`StagesScreen` 数据全部来自 `stageRepo.observeAll()`）无数据可渲染。

## Root Cause（已验证）

- `grep seedIfEmpty` 全工程仅 1 处命中：定义处 `data/knowledge/KnowledgeSeeder.kt:25`，无调用方。
- `RenovationApp.onCreate` 只初始化了 DB / KnowledgeCache / AppContainer，未触发种子写入。
- Web 版对应逻辑在启动时同步执行；Android 移植时遗漏。

## Requirements

1. **启动种子写入**：App 启动时（`RenovationApp.onCreate`）调用 `seeder.seedIfEmpty(today)`，异步执行不阻塞启动；种子表有数据时幂等跳过（`seedIfEmpty` 已内置 count>0 判断）。
2. **UI 即时性**：种子写入完成后流程页自动出现数据（Room Flow 天然支持，无需额外处理；但需确认写入时机不晚于用户切到流程页后仍能刷新——Flow 会自动补发）。
3. **回归防护（spec 沉淀）**：在 `.trellis/spec/android/data-layer.md` 增加「种子写入必须在启动链路被调用」的约定与检查点，避免同类遗漏再次发生。
4. **单测**：为「启动后种子表非空」补一条 Robolectric 测试（可直接测 `seedIfEmpty` 幂等 + 调用链，或测 AppContainer 装配后 stage 表非空）。

## Acceptance Criteria

- [ ] 全新安装（空库）启动后，「流程」页显示 14 阶段列表，可展开看模板任务与验收清单。
- [ ] 二次启动不重复写入（幂等）。
- [ ] 单测覆盖种子写入；lint / unit test 全绿。
- [ ] spec 增加种子调用约定，防止回归。

## Out of Scope

- 首页 / 预算页的其他数据问题（如有另行排查）。
- 种子数据内容本身的修订。

## Technical Notes

- 勘察文件：`RenovationApp.kt`、`KnowledgeSeeder.kt`、`AppContainer.kt`、`StagesScreen.kt`
- `seedIfEmpty` 内部先 `cache.load()`（幂等），再按 count>0 跳过；`today` 参数仅 budget 模板用，种子路径传 `DateUtil.today()` 即可。
- 启动异步执行建议用 `CoroutineScope(SupervisorJob() + Dispatchers.Default)` 持有于 Application，避免 GlobalScope lint 告警。
