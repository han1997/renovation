# 全 App 数据与交互契约

## 1. 范围 / 触发条件
适用于任务 09-10-android-app-polish 及以后对金额、备份、报价、需求规划、初始化、平台文件操作的修改。Android 单机离线架构不变，Room 仍为 v3。报价目录公式保留；明确把自购纳入方案预算比较。

报价状态/计算/校验位于 `domain/quote/`，规划状态/规则位于 `domain/planner/`，备份校验不得反向依赖 UI。迁移包位置不改变 JSON 字段或 Room schema。

## 2. 签名
- `MoneyUtil.parseYuan(text: String): Long?`；`MoneyUtil.input(cents: Long): String`。
- 记账新增/修改用 `BudgetRepository.*Cents`；首次设置用 `HouseProfileRepository.finishOnboardingCents`。旧 Double 入口仅作兼容。
- `ImportExportRepository.exportJson()` / `prepareImport(text): PreparedImport` / `restore(prepared): ImportResult`。
- `QuoteResult.totalCents` 为施工方报价；`estimatedTotalCents = totalCents + summary.selfMainCents`。
- `QuoteBudgetMapping.preview(result, categories, targets): BudgetApplyPreview`；`QuotePlanRepository.applyBudgetPreview(preview)`。
- `FileActionsViewModel.results: Flow<FileActionResult>`，结果为 `Success(message)` / `Cancelled` / `Failure(message)`。
- `PlannerRules.search / normalize / recommend / lines` 为可独立测试的纯函数。

## 3. 数据与交互契约
### 金额
用户输入最多两位小数，精确转换到 Long 分；非法值返回 null，不再按零保存。展示和可回解析字符串固定 Locale.ROOT；不要用浮点拆整数/小数。报价仍按既有逐行整元估算规则取整，记账与备份保留分精度。

### Android 备份 v2
顶层必须包含：`schema_version=2`、`appId`（导出写 `fun.han1997.renovation`；导入兼容旧包名 `com.renovation.guardian`）、`profile`、`taskTemplates`、`taskCompletions`、`customTasks`、`stageOverrides`、`budgetCategories`、`expenses`、`checks`、`notes`、`contacts`、`quickNotes`、`quotePlans`、`plannerState`。
值为对应实体快照，所有金额字段均是 `*Cents: Long`。导出使用一致性读事务；恢复校验后在一个事务中执行。空对象不是合法空备份。

v1 只作读取兼容：元金额转为分，原格式缺少的报价、规划及模板修改保留；确认恢复时重新读取应保留的模块，避免预览期间的新数据被旧快照覆盖。旧格式未记录的完成时间不能推造。恢复不支持 Web 迁移或复活废弃的 space_need。

`task_template` **可被用户修改、新增、删除**，不是纯只读目录。备份必须包含它；初始阶段目录已存在时，任务表为空也不能自动重播种。全量重置恢复默认任务，局部恢复默认保留非内置 ID 任务；外部备份文件不属于重置范围。

### 报价与预算
报价读取房屋面积/上限作为初值，方案内预算编辑不反写房屋。保存记录新 ID；再次保存更新原方案，另存才新增；名称和 mode 元数据与 JSON 同步。

预算应用必须显示大项→分类映射与前后金额。多个大项映射同一分类先合并；未知或无法明确匹配的分类必须由用户选择。人工辅材合计不能擅自拆成清包的人工/辅材比例。事务校验分类仍存在，金额仍是预览原值或已应用目标值；重复确认幂等。其他分类、房屋总上限、已有支出不变。

局部吊顶保留目录面积上限，由输入校验解释，不让 UI 悄悄接受超限数值。防水区分自动估算与显式面积，显式墙面 0 不能回退为估算墙面。未配置的空间不计算空间费用，生成前确认并在清单说明。

### 规划
需求身份保持 itemKey（目录 819 行，816 唯一 ID）；搜索匹配名称或类型，跨空间检索去重。推荐仅针对尚无分配的需求，匹配 presetKey，多个同类房间分别列为建议。预览确认后应用，不改手工结果或重要度。去选、删除空间、改名后归一化引用；未分配独立成组，不能伪装成首个房间。

加载成功后允许编辑；自动保存是串行的最新快照队列，不并发覆盖。保存中/失败状态可见，失败保留编辑并重试；正常退出等待保存完成。

### UI 与平台
沿用 Material 3、动态色/深色、现有字号；静态回退补全 surfaceContainer 色阶，不能混入默认紫色。普通卡片没有空 onClick。当前步骤非法输入阻止继续；回调只能在仓库成功后关闭表单。重复保存采用 singleFlight，不丢弃普通勾选队列。

SAF 待导出文本保存在缓存文件，SavedStateHandle 只保存路径和类型；关闭输出流成功后才报导出成功。取消不报失败。读入后先预览，确认后才恢复；进程回收导致旧导入回调失效时要求重新选择，不清空数据。

长图实际测量行高，拆分长文本并分页（宽度最多 1080px、高度最多 4096px），每张落盘后释放。API 29+ 相册保存，旧系统走 FileProvider 分享，不额外申请存储权限。分享面板打开不等于用户完成分享。

## 4. 校验与错误矩阵
| 输入/状态 | 必须行为 |
|---|---|
| 非法金额、NaN、溢出、分以下精度 | 标红、禁止保存，不自动转零 |
| `{}`、未知备份版本、缺必需字段、无效引用 | 拒绝恢复，原库不变 |
| 恢复中 SQLite 插入失败 | 回滚整次恢复 |
| 预算预览后分类删除/金额被编辑 | 拒绝应用，要求重新预览 |
| 系统文件选择器取消 | Cancelled，不显示成功或失败 |
| 输出流创建/写入/关闭失败 | Failure，不假报导出完成 |
| 种子/目录加载失败 | 明确失败与重试，不永远显示空列表 |
| 需求无可匹配空间 | 留在未分配，不随意分给某房间 |

## 5. Good / Base / Bad
- Good：完整导出→重置→恢复，任务文本/删除状态/日期/完成状态/报价/规划与原快照一致。
- Base：v1 备份缺 quickNotes、报价和规划，保留这些现有模块并告知兼容边界。
- Bad：把任意 JSON 当作默认空对象恢复；把报价总价覆写为房屋总预算；轮流分配需求却叫“按面积推荐”。

## 6. 必须的测试
- `MoneyUtilTest`：0.01/0.29、Locale、非法及 Long 溢出。
- `AppPolishRepositoryTest`：全量往返、v1 预览后仍保留新方案、事务中途失败、幂等预算、上限/支出/非目标分类不变、模板恢复。
- `QuoteCatalogPolishTest`：实际目录每种主材类型、三模式含自购合计、显式防水、吊顶上限、非法状态。
- `QuotePlannerStateTest`：连续保存不重复、mode 更新、快速规划写入不丢最后一版、跨日刷新。
- `PlannerRulesTest`：搜索 OR 语义、同类多房间推荐、保留手工分配、去重/清理与未分配。
- `FormComponentsTest` / `WorkflowComposeTest`：非法草稿阻止继续、整行勾选、普通卡片无假动作、报价保存重开与规划分配。
- `LongImageRendererTest`：超长条目和多页均不超过尺寸上限。
- JVM Compose/截图不是设备验收；真实 SAF、相册/分享、API 24/31/34/35 与动态壁纸取色仍需设备验证。

## 7. Wrong vs Correct
```kotlin
// Wrong：错误变成零；点击导出立即成功；另存与保存混用。
val cents = (text.toDoubleOrNull() ?: 0.0) * 100
snackbar.showSnackbar("导出成功") // 文件选择器尚未结束

// Correct：精确输入、真实结果、稳定方案 ID。
val cents = MoneyUtil.parseYuan(text) ?: return
// CreateDocument 返回 URI 后完成 IO，宿主发 FileActionResult.Success。
// 首次 savePlan 得到的 id 写回 editingPlanId，后续调用 updatePlanState。
```
