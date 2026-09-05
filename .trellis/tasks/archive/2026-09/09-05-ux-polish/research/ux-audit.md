# Research: 装修管家 Android 版 UX 全面体检

- **Query**: 对 android/ 目录（Kotlin + Compose 单 module）做 8 维度 UX 体检，产出可勾选改进清单
- **Scope**: internal（Android 全部 Screen/VM/theme/components + 对照 Web 版 js/views/*）
- **Date**: 2026-09-05
- **对照 spec**: `.trellis/spec/android/ui-theme.md`、`.trellis/spec/android/data-layer.md`

严重度：🔴高（功能缺失/数据丢失风险/明显卡顿） 🟡中（体验明显不佳） 🟢低（打磨项）
工作量：S（≤半天） M（1-2 天） L（>2 天）

---

## 维度 1：加载与空状态

**核心结论**：`EmptyState` 组件（`ui/components/Components.kt:130`）已写好但**全工程零引用**；除随手记外所有列表都没有空状态，首帧一律 `collectAsState(initial = emptyList())` 直接渲染空白。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 1.1 | 🔴 | `ui/stages/StagesScreen.kt:46-47` | 种子写入是异步的（`RenovationApp.kt:39-46`），首次启动进入流程页时 stages 为空 → **整页只有 TopAppBar 的白屏**，无 loading 也无空态提示 | stages 为空且 `seedIfEmpty` 未完成时显示 `LoadingState()`；或给 seeder 加「完成/失败」状态流，空态时显示 EmptyState + 重试 | S |
| 1.2 | 🔴 | `ui/home/HomeScreen.kt:57` | 种子未写入时 `totalTasks=0`、`currentStageName=null`，首页显示「全部完成 🎉」+ 0% 进度——**误导用户** | `totalTasks==0` 时显示「正在准备你的装修计划…」占位而非「全部完成」 | S |
| 1.3 | 🟡 | `ui/budget/BudgetScreen.kt:97-163` | 分类为空时只有「总览 ¥0」卡片，无任何引导（Web 版有「还没有分类，点下面按钮生成推荐分类」，`js/views/budget.js:74`） | categories 为空时显示 EmptyState + 「生成推荐分类」按钮（可复用 `buildBudgetTemplate`） | S |
| 1.4 | 🟡 | `ui/more/MoreScreen.kt:129,151,251` | 联系人 / 笔记 / 空间需求三个 section 无空状态（Web 版有友好文案：`js/views/more.js:37,57,72`） | 各 section 空时显示一行引导文案（如「把工长、设计师的电话存在这里…」） | S |
| 1.5 | 🟢 | `ui/home/HomeScreen.kt:108-110,124-127` | 「逾期/今天/未来 7 天」三组标题**永远渲染**，空组显示「暂无任务」，浪费纵向空间（Web 版空组直接隐藏，`js/views/home.js:126`） | 空组跳过不渲染；三组全空时显示一条聚合空态（Web 有「未来 7 天暂无安排，先去流程页查看」） | S |
| 1.6 | 🟢 | `ui/guide/GuideScreen.kt:53-55` | `knowledge == null` 时显示「资料加载中…」，但 knowledge 在启动时同步加载（`RenovationApp.kt:34`），null 只可能是加载失败——文案误导且无重试 | 改为「资料加载失败」+ 重试按钮 | S |
| 1.7 | ✅ | `ui/more/MoreScreen.kt:169-178` | 随手记空状态已实现（近期改动），是全 app 唯一像样的空态，可作为其他 section 的模板 | — | — |

---

## 维度 2：反馈与确认

**核心结论**：删除确认**只有 2 处**（分类删除、清空全部数据）；其余 5 类删除全部一键直删且无 Snackbar 撤销；保存/导出成功**零反馈**。Web 版所有删除都有 `UI.confirmDlg` + `UI.toast('已删除')`。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 2.1 | 🔴 | `ui/more/MoreScreen.kt:151-158` + `ui/more/MoreViewModel.kt:53` | **笔记根本无法删除**：列表卡片点击只能进编辑，NoteDialog 只有保存/取消；`deleteNote()` 是死代码 | 笔记卡片加删除入口（编辑弹窗内「删除」按钮或列表滑动删除），带确认 | S |
| 2.2 | 🔴 | `ui/budget/BudgetScreen.kt:157` | 支出删除无确认（Web 版有「确定删除「xx」这笔记录？」，`js/views/budget.js:342`）——误触即丢钱记录 | 加确认对话框，或删除后 Snackbar + 撤销（保留实体延迟删除） | S |
| 2.3 | 🔴 | `ui/more/MoreScreen.kt:138`（联系人）、`:548`（随手记）、`:259`（空间需求）、`ui/stages/StagesScreen.kt:145`（自定义任务） | 四处删除均无确认、无撤销（Web 版均有 confirmDlg：`js/views/more.js:165,211`、`js/views/stages.js:290`） | 统一封装 `ConfirmDeleteDialog` 组件复用；或统一 Snackbar 撤销模式 | M |
| 2.4 | 🟡 | 全工程 | **保存成功无任何反馈**：新增/编辑支出、分类、联系人、笔记、随手记、房屋信息后对话框关闭即完事（Web 版到处是 toast：`js/app.js:360`、`js/views/budget.js:204`「已记录 ¥xx ✅」） | Budget/More 页已有 SnackbarHost（`BudgetScreen.kt:91`、`MoreScreen.kt:96`），保存成功后 `showSnackbar("已保存")`，成本极低 | S |
| 2.5 | 🟡 | `ui/nav/AppRoot.kt:123-131` + `ui/budget/BudgetScreen.kt:80` | CSV/JSON 导出**静默执行**：成功无提示、失败被 catch 吞掉（注释自己承认「静默忽略」）；Web 版有「已导出 CSV 📄」（`js/views/budget.js:237`） | 导出回调里成功/失败都 showSnackbar | S |
| 2.6 | 🟡 | `ui/more/MoreScreen.kt:324-332` | 「清除全部数据」只有一层确认；Web 版是两层确认 + 提示先导出备份（`js/views/more.js:236-243`） | 确认文案加「建议先导出备份」；或二次确认 | S |
| 2.7 | ✅ | `ui/budget/BudgetScreen.kt:191-204` | 分类删除有确认框 + 「分类下还有支出，无法删除」Snackbar——全 app 反馈做得最好的一处，可作为统一范式 | — | — |

---

## 维度 3：输入体验

**核心结论**：全工程 **0 处 `imePadding`**（grep 证实）；表单校验是「静默失败」模式——非法输入点保存后对话框关闭、什么都没发生、也没有错误提示。随手记弹窗的 `enabled = content.isNotBlank()` 是唯一做对的（`MoreScreen.kt:584`）。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 3.1 | 🔴 | `ui/stages/StagesScreen.kt:150-160` | 流程页展开详情底部的「添加自定义任务」输入框：键盘弹出时**被 IME 遮挡**（Scaffold 无 `imePadding`，edge-to-edge 下 adjustResize 不生效） | Scaffold content 加 `.imePadding()`；配合 `windowSoftInputMode="adjustResize"` | S |
| 3.2 | 🔴 | `ui/budget/BudgetScreen.kt:219,252` + `ui/budget/BudgetViewModel.kt:23,39` | 新增支出/分类：名称留白或金额非法时，VM `if (name.isBlank()) return` **静默丢弃**，对话框已关闭，用户以为保存了 | 保存前校验：名称为空时 TextField `isError + supportingText`，不关闭对话框；参照 QuickNoteDialog 禁用保存按钮 | S |
| 3.3 | 🟡 | `ui/budget/BudgetScreen.kt:215,246` + `ui/more/MoreScreen.kt:368` | 金额回显用 `(cents / 100.0).toString()`：≥1e7 元时 Double.toString 产出科学计数法（如 `1.23456789E7`）；且允许负数、多个小数点 | 回显改 `"%.2f".format(...)`；`onValueChange` 过滤非法字符（正则 `\d*\.?\d{0,2}`） | S |
| 3.4 | 🟡 | `ui/budget/BudgetScreen.kt:239-269` | 支出弹窗**没有分类选择器**：编辑支出时无法改分类（Web 版有分类下拉，`js/views/budget.js:187`） | 弹窗加分类 ExposedDropdownMenu（MoreScreen 的 `LabeledDropdown` 可直接复用） | M |
| 3.5 | 🟡 | `ui/onboarding/OnboardingScreen.kt:128,133` | 向导校验静默失败：面积/档次不合法时点「下一步」无反应，无错误文案；面积无范围校验（Web 版校验 15–2000㎡，`js/app.js:288`）；总预算允许 0 | 每步加 `isError + supportingText`（「请填写 15–2000 的面积」）；按钮 disabled 态 + 说明为什么 | S |
| 3.6 | 🟢 | `ui/stages/StagesScreen.kt:157` | 添加任务：无 `KeyboardActions(imeAction = Done)` 回车提交；输入为空时也清空文本框（无害但粗糙） | 加 `KeyboardOptions(imeAction = ImeAction.Done)` + `KeyboardActions { onDone }`；空输入不清空 | S |
| 3.7 | 🟢 | `ui/components/DatePickerField.kt:46-54` | 只读 TextField 上叠 `.clickable{}`：点击空白区域才触发，TalkBack 语义合并不佳 | 改用 `Modifier.pointerInput` 或 `textField` 的 `readOnly + onClick`（M3 支持）；trailing icon desc 为 null 可接受 | S |
| 3.8 | 🟢 | `ui/more/MoreScreen.kt:445-448` | 联系人电话字段用默认文本键盘（无 `KeyboardType.Phone`）；无格式校验 | 电话字段加 `KeyboardType.Phone` | S |

---

## 维度 4：导航与返回

**核心结论**：底部导航 `saveState/restoreState` 配置正确（滚动位置可恢复，因 `rememberLazyListState` 内部是 rememberSaveable）；但所有 `remember { mutableStateOf }` 的 UI 状态切 Tab 即丢；Onboarding 完成后**无法重跑**，且「编辑房屋信息」不会触发预算模板重算（Web 版会询问）。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 4.1 | 🟡 | `ui/stages/StagesScreen.kt:48`、`ui/budget/BudgetScreen.kt:64-69`、`ui/guide/GuideScreen.kt:41` | `expandedId`（展开的阶段/分类）、Guide 的 `tab` 用 `remember` 而非 `rememberSaveable`——切 Tab 再回来，展开状态/指南子页全部重置 | 全部改 `rememberSaveable`（expandedId 是 String? 可直接保存；Guide tab 是 Int） | S |
| 4.2 | 🟡 | `ui/nav/AppRoot.kt:152-156` + `ui/more/MoreScreen.kt:290-295` | Onboarding 完成后无重入口；「我的→编辑」改面积/方式/档次后**预算分类不会重算**（`HouseProfileRepository.updateProfile` 只改 profile），与 Web 版「要重算预算吗？」流程不一致（`js/views/more.js:133-148`）；`alignTotalToCategoriesSum()`（`HouseProfileRepository.kt:107`）也是死代码 | 编辑弹窗保存后若面积/方式/档次变更，弹「按新信息重算分类预算？」确认框，调 `finishOnboarding` 的模板合并逻辑 | M |
| 4.3 | 🟢 | `ui/nav/AppRoot.kt:190-196` | 底部导航 `popUpTo(startDestination) + saveState + restoreState + launchSingleTop` 是标准写法，无问题；返回键行为符合预期（回首页再退出） | — | — |
| 4.4 | 🟢 | `ui/home/HomeScreen.kt:129-142` | 首页任务卡片点击无跳转（Web 版点击跳到对应阶段，`js/views/home.js:118` `data-target="stages"`）——用户看到逾期任务却要自己去流程页找 | 任务卡片 onClick 导航到 stages 并展开对应阶段（需 NavHost 传参或共享 VM） | M |

---

## 维度 5：视觉一致性

**核心结论**：`SectionCard` 复用良好（5 个 Screen 全在用）；屏幕代码**零硬编码颜色**（grep `Color(0x` 仅 theme 文件命中），深色模式安全；edge-to-edge 的状态栏/导航栏由 Scaffold+TopAppBar+NavigationBar 正确处理，**唯一遗漏是 FAB 遮挡内容**。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 5.1 | 🟡 | `ui/budget/BudgetScreen.kt:86-90,93-95` | FAB 悬浮在列表上，LazyColumn 无底部额外 padding——**滚动到底时最后一个分类卡片被 FAB 遮挡**（Scaffold 的 innerPadding 不含 FAB 高度） | LazyColumn 加 `contentPadding = PaddingValues(bottom = 96.dp)` | S |
| 5.2 | 🟡 | `ui/stages/StagesScreen.kt:68-74,111-177` | 展开的阶段详情（目标/避坑/任务清单）直接铺在页面背景上，不在任何卡片容器内——与首页/预算/我的的卡片风格割裂，视觉上像「漏出来的内容」 | 详情内容包进 SectionCard 或加 surface 色背景容器 | S |
| 5.3 | 🟢 | `ui/home/HomeScreen.kt:46` | 首页 TopAppBar 标题「装修进行时」与其他页（流程/预算/指南/我的）命名风格不一致，且 `strings.xml:20` 的 `topbar_default_subtitle`（新房装修全流程助手）未被使用 | 统一为「首页」或补副标题；未用 string 资源清理或启用 | S |
| 5.4 | 🟢 | `ui/budget/BudgetScreen.kt:81`、`ui/more/MoreScreen.kt:271,279` | 图标语义混乱：CSV 导出用 `Icons.Filled.Upload`；「导出」用 Upload、「导入」用 Download（方向反了——导出是下载到文件） | 导出用 `FileUpload`→改 `Download`/`SaveAlt`，导入用 `Upload`/`OpenInNew`，或统一用文字按钮 | S |
| 5.5 | 🟢 | `ui/onboarding/OnboardingScreen.kt:258-296` | 自造 `SelectableCard` 与 `SectionCard` 风格相近但圆角/选中态独立维护 | 可合并进 components（低优先级，向导只用一次） | S |
| 5.6 | ✅ | `ui/theme/Theme.kt:27-34`、`Color.kt` | 动态取色 + 7-11 静态回退色板完整（含 dark 全套）；`values-night/themes.xml` 启动主题也配了，无白闪 | — | — |

---

## 维度 6：性能与流畅度

**核心结论**：LazyColumn key 使用规范（全部 items 都有 key，grep 证实）；种子写入已异步不阻塞首帧，但 **85KB JSON 在主线程同步解析**；预算页展开分类的 Flow 每次重组重建。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 6.1 | 🟡 | `RenovationApp.kt:34` | `KnowledgeCache(this).also { it.load() }` 在 `onCreate` 主线程同步读 + 解析 85KB knowledge.json + 12KB prices.json——低端机首帧延迟可感知 | `load()` 挪到 appScope 协程（Default dispatcher）；knowledge 为 null 的 UI 已有兜底（GuideScreen），但 Onboarding/More 依赖 tiers/modes 需加 loading 态 | M |
| 6.2 | 🟡 | `ui/budget/BudgetScreen.kt:145` | `vm.observeExpenses(cat.id).collectAsState()` 写在 `items` lambda 内：每次该 item 重组都 new 一个 Room Flow 实例，`produceState` 以 flow 为 key → **重启收集、重查数据库** | `val expensesFlow = remember(cat.id) { vm.observeExpenses(cat.id) }` | S |
| 6.3 | 🟢 | `ui/stages/StagesViewModel.kt:52-64` | 每个模板任务一条 completion Flow 再 combine——N 个任务 N 条订阅；当前数据量（每阶段约 5-10 任务）无碍，但展开多阶段时订阅数放大 | 可改为一条 JOIN 查询（observeTemplatesWithCompletion）；非必需 | M |
| 6.4 | ✅ | `RenovationApp.kt:39-46` | 种子写入在 `Dispatchers.Default` 异步执行且幂等，不阻塞首帧（但见 1.1 的空屏副作用） | — | — |
| 6.5 | ✅ | 全部 LazyColumn | `items(..., key = { it.id })` 全覆盖（HomeScreen:129、StagesScreen:57、BudgetScreen:122、MoreScreen:129/200/230/251、GuideScreen 各 tab），MoreScreen 分组标题也有字符串 key | — | — |

---

## 维度 7：文案与本地化

**核心结论**：`strings.xml` 只有 17 条且**大半未被引用**（tab 名在 `AppRoot.kt:52-56` 硬编码、`topbar_default_subtitle`/`empty_no_profile`/`cta_start_wizard`/`action_*` 全部闲置）；其余文案全部硬编码在 Kotlin 里。纯中文 app 可接受，但「半吊子 strings.xml」状态最差——要么用起来要么删掉。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 7.1 | 🟡 | `res/values/strings.xml` vs `ui/nav/AppRoot.kt:52-56` | `tab_home`~`tab_more` 已定义但 TopLevelRoute 硬编码「首页/流程/预算/指南/我的」；`action_save/cancel/delete/confirm` 定义了但所有对话框仍写「保存/取消」 | 二选一：TopLevelRoute 与通用按钮改用 stringResource；或删掉未用 string 保持诚实 | S |
| 7.2 | 🟢 | `ui/more/MoreScreen.kt:282` vs Web `js/views/more.js:91` | 「清除全部数据」vs Web「清空全部数据」——动词不一致 | 与 Web 对齐为「清空全部数据」 | S |
| 7.3 | 🟢 | `ui/onboarding/OnboardingScreen.kt:248` | 「参考推荐总预算：约 ¥xx（可在我的页修改）」——但「我的页」改总预算入口是「编辑房屋信息」弹窗里的「总预算（元）」字段，指引不够直接 | 文案改「可在『我的 → 编辑』中修改」 | S |
| 7.4 | 🟢 | `ui/home/HomeScreen.kt:46` | 首页标题「装修进行时」是 Android 独有创意文案，Web 版无此概念；若保留建议加副标题说明 | 保留或统一，见 5.3 | S |
| 7.5 | 🟢 | 术语表 | Android 与 Web 术语基本对齐（阶段/分类/支出/验收清单/避坑）；「随手记」是 Android 新增功能 Web 没有，无冲突 | — | — |

---

## 维度 8：无障碍与细节

**核心结论**：所有 IconButton 都有 contentDescription（好）；主要缺口是 **Checkbox 行的文本不可点**（点击目标过小）和选中态语义缺失。

| # | 严重度 | 位置 | 问题 | 建议 | 工作量 |
|---|---|---|---|---|---|
| 8.1 | 🟡 | `ui/home/HomeScreen.kt:131-140`、`ui/stages/StagesScreen.kt:133-139,141-148,168-173` | 任务行的点击热区只有 Checkbox 本身（约 48dp 方块），**点文字无反应**——移动端高频误操作点；Web 版整行 label 可点（`js/views/home.js:119-120`） | Row 加 `Modifier.toggleable(value, role = Role.Checkbox, onValueChange)`，整行可点且 TalkBack 合并朗读 | S |
| 8.2 | 🟡 | `ui/onboarding/OnboardingScreen.kt:266-296` | `SelectableCard` 选中态只有视觉（primaryContainer + CheckCircle），无 `semantics { selected = true }`——TalkBack 用户听不出选没选 | Card 加 `Modifier.semantics { selected = ... }` 或改用 `Modifier.selectable(role = Role.RadioButton)` | S |
| 8.3 | 🟢 | `ui/more/MoreScreen.kt:417-419` | 风格选中指示用 `Icons.Filled.Edit`（desc=null）——语义是「编辑」不是「已选中」 | 换 `Icons.Filled.Check` + contentDescription = "已选择" | S |
| 8.4 | 🟢 | `ui/nav/AppRoot.kt:198-199` | NavigationBarItem 的 icon contentDescription 与 label 文本相同——TalkBack 朗读两遍 | icon 的 desc 设为 null（label 已可读） | S |
| 8.5 | 🟢 | `ui/more/MoreScreen.kt:528`、`ui/stages/StagesScreen.kt:134` | 勾选完成无触觉反馈（Web 版有 toast 庆祝「🎉 阶段完成」） | 勾选时 `LocalHapticFeedback.performHapticFeedback`；阶段 100% 时 Snackbar 庆祝 | S |
| 8.6 | ✅ | 全部 IconButton | 删除/编辑/新增图标的 contentDescription 全部有中文描述（grep 证实仅 3 处装饰性图标为 null，均合理） | — | — |

---

## Top 10 建议优先修复

按 **用户感知度 × 严重度 ÷ 工作量** 排序（S 项优先，高频路径优先）：

- [ ] **1. 笔记无法删除**（2.1）——功能缺失 + 死代码，用户建错笔记永远删不掉。`MoreScreen.kt:151` 加删除入口。**S**
- [ ] **2. 保存静默失败 + 无错误提示**（3.2/3.5）——填错表单点保存=什么都没发生，用户会以为 app 坏了。校验 + isError + 禁用保存按钮（抄 QuickNoteDialog 的 `enabled` 模式）。**S**
- [ ] **3. 删除操作加确认或撤销**（2.2/2.3）——支出/联系人/随手记/空间/自定义任务 5 处一键直删。封装一个 ConfirmDeleteDialog 全部接入。**M**
- [ ] **4. 首启流程页白屏 + 首页误报「全部完成 🎉」**（1.1/1.2）——新用户第一印象。空态/loading 兜底。**S**
- [ ] **5. IME 遮挡输入框**（3.1）——流程页添加任务时键盘盖住输入框，功能基本不可用。Scaffold + imePadding。**S**
- [ ] **6. 保存/导出成功反馈**（2.4/2.5）——SnackbarHost 已就位，只差 showSnackbar 调用，性价比最高。**S**
- [ ] **7. 任务行整行可点**（8.1）——首页/流程页勾任务是最高频操作，点文字没反应最招人烦。toggleable 语义。**S**
- [ ] **8. 切 Tab 丢展开/子页状态**（4.1）——rememberSaveable 三处替换，5 分钟修完的体验提升。**S**
- [ ] **9. 预算页空态 + FAB 遮挡**（1.3/5.1）——分类为空无引导 + 底部卡片被 FAB 盖住，同屏两个问题一起修。**S**
- [ ] **10. 编辑房屋信息后预算不重算**（4.2）——改了面积/档次预算纹丝不动，与 Web 行为不一致，用户会不信任数据。重算确认框 + 模板合并。**M**

**次优先（Top 10 之外但值得排期）**：金额输入科学计数法/负数过滤（3.3）、支出弹窗分类选择器（3.4）、向导面积范围校验（3.5）、KnowledgeCache 异步加载（6.1）、BudgetScreen Flow remember（6.2）、strings.xml 清理（7.1）、阶段详情卡片化（5.2）。

---

## Caveats / Not Found

- 未实际运行 app，所有「白屏/遮挡」结论基于代码推演（edge-to-edge + 无 imePadding 的行为在 API 35 上是确定性的，但建议真机验证一次）。
- Web 版 `js/ui.js` 的 `UI.confirmDlg`/`UI.toast` 具体样式未深入，仅确认行为存在。
- `Icons.Filled.Upload/Download` 的语义判断带主观成分（5.4），可由设计定夺。
- 性能项（6.1/6.3）未做 profiling，严重度按经验估计。
