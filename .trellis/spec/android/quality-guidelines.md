# Quality Guidelines

## Overview

测试分三层：JVM 单元测试（Robolectric / MockK / Turbine）、Instrumented（Room 真机 / 模拟器）、Compose UI 测试。DoD 要求 lint / typecheck / 单测 / 仪器测试通过、APK 可签名构建。

## 测试分层

| 层 | 位置 | 工具 | 覆盖 |
|----|------|------|------|
| DAO 聚合 | `app/src/test/.../data/db/` | Robolectric + in-memory Room | 首页三段分组 / 阶段完成度 / 分类超支 / CSV 扁平视图 |
| Repository 导入导出 | `app/src/test/.../data/repo/` | Robolectric + in-memory Room | JSON 导出 / 导入原子性 |
| 纯函数 | `app/src/test/.../ui/budget/` | JUnit | `buildCsv` CSV 拼装 |
| ViewModel | `app/src/test/.../ui/*/` | MockK + Turbine | 首次设置 / 首页 / 预算逻辑 |
| Compose UI | `app/src/androidTest/` | compose-ui-test | 关键交互流 |

## 约定

- Room 单测用 `Room.inMemoryDatabaseBuilder` + `allowMainThreadQueries()`。
- ViewModel 测试注入 `mockk<AppContainer>(relaxed = true)` + `mockk<RenovationApp>`，stub `container.budgetRepo` 等 Flow 返回值。
- 纯函数测试不依赖 Android 运行时。

## 验证清单

提交前在 `android/` 下运行：

```bash
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:lintDebug
gradlew.bat :app:assembleDebug
```

涉及 DB schema 或 Room 变更时，额外确认 `app/schemas` 下的导出 schema 已更新。

## 手测清单（DoD）

首次设置 → 首页打卡 → 流程勾选 → 预算录入 → CSV 导出 → JSON 导入/导出 → 数据重置；并在 Android 7.0 / 12 / 14 三档真机或模拟器跑通主流程。

## 国产 ROM 注意

`dynamicColorScheme` 在 MIUI/HyperOS、ColorOS、OriginOS 上支持度未实测，改主题后需在真机验证「换壁纸 → App 颜色变化」。