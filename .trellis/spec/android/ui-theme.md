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

## Theme（Material You）

- 入口：`ui/theme/Theme.kt` 的 `AppTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = ...)`。
- Android 12+：`dynamicLightColorScheme` / `dynamicDarkColorScheme` 取系统壁纸色。
- Android 7–11：回退静态色板（陶土橙 `cf6b45` 系 + 米色 `#f6f3ee`，与 Web 品牌一致），见 `ui/theme/Color.kt`。
- Shape / Type 见 `ui/theme/Shape.kt` / `Type.kt`。
- 不做 App 内暗色切换（跟随系统），不引入 Accompanist。

## 边界与 edge-to-edge

targetSdk 35 强制 edge-to-edge，所有 Screen 需按 `WindowInsets` 处理安全区，避免内容被状态栏 / 手势条遮挡。