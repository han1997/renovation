# UI & Theme

## Overview

纯 Compose。每个 Tab 一个 `*Screen.kt` + `*ViewModel.kt`，导航集中在 `ui/nav/AppRoot.kt`。

## Screen / ViewModel 对应

| Tab | Screen | ViewModel |
|-----|--------|-----------|
| 首页 | `ui/home/HomeScreen.kt` | `ui/home/HomeViewModel.kt` |
| 流程 | `ui/stages/StagesScreen.kt` | `ui/stages/StagesViewModel.kt` |
| 预算 | `ui/budget/BudgetScreen.kt` | `ui/budget/BudgetViewModel.kt` |
| 指南 | `ui/guide/GuideScreen.kt` | （无独立 VM，读 `knowledge`） |
| 我的 | `ui/more/MoreScreen.kt` | `ui/more/MoreViewModel.kt` |
| 首次设置 | `ui/onboarding/OnboardingScreen.kt` | `ui/onboarding/OnboardingViewModel.kt` |

通用 ViewModel 基类：`ui/AppViewModel.kt`（`AndroidViewModel`，暴露 `container: AppContainer`）。

## 约定

- ViewModel 持有状态用 `Flow` 暴露只读，业务写操作在 `viewModelScope.launch` 里调用 Repository。
- Screen 只负责渲染 + 回调，不直接触碰 Room / 知识数据。
- 日期由 `container.todayProvider` 注入（`DateUtil.today()`），避免 UI 层自行取系统时间，便于测试覆盖。
- 平台能力（SAF 文件选择 / 分享）在 Screen 层用 `rememberLauncherForActivityResult`，ViewModel 只提供纯数据（如 `exportJsonString()`）。

## 交互反馈约定（UX 体检后沉淀，任务 09-05-ux-polish）

- **删除必须确认**：一律复用 `ui/components/ConfirmDeleteDialog.kt`（M3 AlertDialog，默认文案「删除后不可恢复，确定删除吗？」）。新增删除入口时禁止一键直删。
- **写操作要有 Snackbar 反馈**：各 Tab 的 SnackbarHost 已就位，保存/删除/导入/导出成功与失败都应 `showSnackbar`（文案简短中文：「已保存」「导出完成」「导入失败：原因」）。
- **表单校验不可静默失败**：非法输入（名称空、金额 ≤0）必须 `isError = true` + `supportingText` 错误文案 + 禁用保存按钮（参照 `QuickNoteDialog` 的 `enabled` 模式）。
- **含输入框的对话框**：内容区加 `verticalScroll(...) + Modifier.imePadding()`，防止键盘遮挡（edge-to-edge 下 `adjustResize` 不生效）。
- **空列表要有占位**：首帧 `collectAsState(initial = emptyList())` 渲染空白不可接受——种子数据加载中给 `CircularProgressIndicator`，业务数据为空给引导文案（`EmptyState` 组件在 `ui/components/Components.kt`）。

### 金额/数字格式化必须 Locale.ROOT（Common Mistake）

**Symptom**：编辑对话框预填金额变成 `123,45`（逗号小数点），`toDoubleOrNull()` 解析失败，无法保存。

**Cause**：`"%.2f".format(...)` / `String.format("%.2f", ...)` 不带 Locale 时用系统默认 Locale——逗号小数点地区（de、fr 等）产出逗号，且该字符串后续要 `toDoubleOrNull()` 回解析。

**Fix**（Wrong vs Correct）：

```kotlin
// Wrong —— Locale 敏感，回解析必炸
"%.2f".format(cents / 100.0)

// Correct —— 机器可回解析的字符串一律 Locale.ROOT
String.format(Locale.ROOT, "%.2f", cents / 100.0)
```

规则：**给人看**的金额展示用 `MoneyUtil.format/formatFull`；**要回解析**（对话框预填、CSV）的数字字符串一律 `Locale.ROOT`。位置：`BudgetScreen.kt`、`MoreScreen.kt` 金额编辑预填处。

## Theme（Material You）

- 入口：`ui/theme/Theme.kt` 的 `AppTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = ...)`。
- Android 12+：`dynamicLightColorScheme` / `dynamicDarkColorScheme` 取系统壁纸色。
- Android 7–11：回退静态色板（陶土橙 `cf6b45` 系 + 米色 `#f6f3ee`，与 Web 品牌一致），见 `ui/theme/Color.kt`。
- Shape / Type 见 `ui/theme/Shape.kt` / `Type.kt`。
- 不做 App 内暗色切换（跟随系统），不引入 Accompanist。

## 边界与 edge-to-edge

targetSdk 35 强制 edge-to-edge，所有 Screen 需按 `WindowInsets` 处理安全区，避免内容被状态栏 / 手势条遮挡。