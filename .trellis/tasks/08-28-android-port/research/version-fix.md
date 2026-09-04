# Version Fix: Kotlin 2.3.11 无效 → Kotlin 2.3.20 + KSP 2.3.11

- **Task**: `.trellis/tasks/08-28-android-port`
- **Date**: 2026-09-04
- **Type**: 构建修复（版本组合）

## 问题

`android/gradle/libs.versions.toml` 原写 `kotlin = "2.3.11"`，构建立即失败：

```
Plugin [id: 'org.jetbrains.kotlin.android', version: '2.3.11'] was not found
```

## 证据链（Maven Central 元数据，2026-09-04 抓取）

1. `kotlin-gradle-plugin` maven-metadata 显示 **Kotlin 无 2.3.11**；稳定线为
   `2.3.0 / 2.3.10 / 2.3.20 / 2.3.21 / 2.4.0 / 2.4.10`。
2. `com.google.devtools.ksp.gradle.plugin` maven-metadata 显示 **KSP 新版编号
   2.3.x 起与 Kotlin 版本号解耦**，最新 stable = `2.3.11`（2026-08-03 发布）。
3. `symbol-processing-common-deps:2.3.11` POM 声明依赖
   **`kotlin-stdlib 2.3.20`** → 配对关系为 **Kotlin 2.3.20 + KSP 2.3.11**。

## 修复

`android/gradle/libs.versions.toml`：

- `kotlin = "2.3.11"` → `kotlin = "2.3.20"`
- `ksp` 保持 `2.3.11`
- 同步修正版本块注释（原注释称「采用 Kotlin 2.3.11 / KSP 2.3.11」是错误的）

## 验证结果

- `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL**
  - 产物：`android/app/build/outputs/apk/debug/app-debug.apk`（19,961,072 bytes，2026-09-04 12:02 构建，全部任务 up-to-date）
- `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL**
  - 8 tests / 0 failures / 0 skipped（AggregatesTest、BudgetViewModelTest、HomeViewModelTest、OnboardingViewModelTest）

## 备注

- 未升 Kotlin 2.4.10：KSP 2.4.x 尚未发布，当前代码基于 Kotlin 2.3.x 编写，升版风险大。KSP 2.4.x 发布后可从 2.3.20 → 2.4.10 平滑升级。
- Compose 编译器 plugin `org.jetbrains.kotlin.plugin.compose` 跟随 Kotlin 版本，无需单独配置。