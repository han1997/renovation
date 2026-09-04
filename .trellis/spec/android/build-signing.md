# Build & Signing

## Overview

构建与签名约定的事实源是 `android/gradle/libs.versions.toml`、`android/app/build.gradle.kts`、`android/build.gradle.kts` 与 `android/README.md`。

## 版本目录（libs.versions.toml）

唯一权威，升级时以 `.toml` 内注释的配对约束为准：

- AGP `9.3.2`；Kotlin `2.3.20`；KSP `2.3.11`（与 Kotlin 2.3.20 配对）。
- Compose BOM `2026.08.00`；Room `2.8.4`；Navigation `2.9.5`。
- serialization-json `1.9.0`；datetime `0.7.1`；coroutines `1.10.2`。
- minSdk `24`；targetSdk `35`；compileSdk `37`。

## 构建命令

在 `android/` 下执行：

```bash
gradlew.bat :app:assembleDebug            # 调试包
gradlew.bat :app:assembleRelease          # 发布包
gradlew.bat :app:testDebugUnitTest        # 单元测试
gradlew.bat :app:connectedDebugAndroidTest # Instrumented / Compose UI 测试
gradlew.bat :app:lintDebug                # Lint
```

## 签名

- **debug**：SDK 默认 debug keystore，无需配置。
- **release**：当前 `app/build.gradle.kts` 中 `release` 复用 debug 签名（`signingConfigs.getByName("debug")`）。
  - 上架前切换独立 release keystore：文件放仓库外，密码经 `gradle.properties` 环境变量注入，不硬编码、不进 git。
  - 字段名与安全约定见 `android/keystore/README.md`。

## KSP / Room schema

- `app/build.gradle.kts` 中 `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`，schema 落 `app/schemas` 便于 review。
- Room `exportSchema = true`。

## 关键插件特性

- AGP 9.x 内置 Kotlin，**不再**引入 `org.jetbrains.kotlin.android` 插件（引入会报错），见 `android/build.gradle.kts` 注释。
- 根构建文件只 `alias(...) apply false` 声明，实际应用在 `:app`。
- `kotlin.plugin.compose`、`kotlin.plugin.serialization`、`ksp` 为应用插件。