# 装修管家 Android 版

原生 **Kotlin + Jetpack Compose** 开发的“装修管家”客户端，是项目唯一持续开发的应用端。

> **维护状态（2026-09-05）：网页版已封存，后续仅专注 Android App。** 仓库根目录的 Web 源码仅作历史参考，不再新增功能或进行常规维护；Android 不再承担双端功能对齐或向 Web 同步改动的要求。项目整体说明见 [根 README](../README.md)。

- 数据本地化优先，无后端、无登录、无云同步；换机靠 Android 端 JSON 备份 / 恢复（不等同于 Web 备份迁移）。
- 面向中国大陆用户（简体中文），不强依赖 Google Play / GMS。
- MVP 覆盖五 Tab（首页 / 流程 / 预算 / 指南 / 我的）+ 首次设置向导 + JSON 备份/恢复 + CSV 导出。

## 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| AGP | 9.3.2 | Gradle 内置 Kotlin，不再单独引入 `kotlin-android` 插件 |
| Kotlin | 2.3.20 | 见 `.\gradle\libs.versions.toml` 注释（KSP 配对原因） |
| KSP | 2.3.11 | 与 Kotlin 2.3.20 配对（其 POM 依赖 `kotlin-stdlib` 2.3.20） |
| Compose BOM | 2026.08.00 | Material 3 |
| Room | 2.8.4 | SQLite，单 module，schema 落 `app/schemas` |
| Navigation Compose | 2.9.5 | 五 Tab + 首次设置向导 |
| kotlinx-serialization-json | 1.9.0 | JSON 备份、知识数据解析 |
| kotlinx-datetime | 0.7.1 | 日期处理 |
| minSdk | 24 | Android 7.0+（保证 4 字节 emoji 彩色渲染） |
| targetSdk | 35 | 强制 edge-to-edge |
| compileSdk | 37 | Compose 1.12.0 AAR 元数据要求 ≥ 37 |

> 版本事实源是 `gradle/libs.versions.toml`，上表为当前锁定值；升级时以 `.toml` 内注释说明的配对约束为准（尤其 Kotlin ↔ KSP）。

## 构建命令

在 `android/` 目录下执行（Windows 用 `gradlew.bat`，Linux/macOS 用 `./gradlew`）：

```bash
# 调试包（默认 debug keystore 签名；applicationId 追加 .debug 后缀）
gradlew.bat :app:assembleDebug

# 发布包（当前 release 复用 debug 签名，见下方「签名约定」）
gradlew.bat :app:assembleRelease

# 单元测试（JVM，含 Room DAO / ViewModel / 纯函数）
gradlew.bat :app:testDebugUnitTest

# Instrumented / Compose UI 测试（需真机或模拟器）
gradlew.bat :app:connectedDebugAndroidTest

# Lint
gradlew.bat :app:lintDebug
```

APK 输出路径：

- 调试包：`app/build/outputs/apk/debug/app-debug.apk`
- 发布包：`app/build/outputs/apk/release/app-release.apk`

## 调试命令

```bash
# 列出已连接设备 / 模拟器
adb devices

# 安装调试包
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动主界面
adb shell am start -n com.renovation.guardian/.MainActivity

# 清空应用数据（回到首次设置向导）
adb shell pm clear com.renovation.guardian

# 抓取日志
adb logcat | findstr renovation
```

## 模块结构

```text
android/
├─ app/
│  └─ src/
│     ├─ main/
│     │  ├─ assets/
│     │  │  ├─ knowledge.json   # 只读知识数据（流程/避坑/风格/百科/空间需求）
│     │  │  └─ prices.json      # 只读价格数据（档位/档次/参考价）
│     │  ├─ java/com/renovation/guardian/
│     │  │  ├─ data/db/         # Room 实体 / DAO（12 张表）
│     │  │  ├─ data/knowledge/  # 知识数据 JSON 反序列化 + 内存缓存 + 种子写入
│     │  │  ├─ data/repo/       # 仓库层（业务聚合、导入导出）
│     │  │  ├─ ui/              # Compose 界面（home/stages/budget/guide/more/onboarding）
│     │  │  ├─ ui/theme/        # Material You 主题（动态取色 + 静态回退色板）
│     │  │  └─ util/            # MoneyUtil / DateUtil / IdGen
│     │  └─ res/                # 资源
│     ├─ test/                  # JVM 单元测试（Room DAO / ViewModel / 纯函数）
│     └─ androidTest/           # Instrumented / Compose UI 测试
├─ gradle/libs.versions.toml    # 版本目录（唯一版本事实源）
├─ keystore/                    # 签名约定（见 README.md）
└─ schemas/                     # Room 导出 schema（app/schemas）
```

## 签名约定

- **debug**：使用 Android SDK 默认 debug keystore（`~/.android/debug.keystore`），无一需配置。
- **release**：当前 `app/build.gradle.kts` 里 `release` 构建类型 `signingConfig = signingConfigs.getByName("debug")` —— 即 release 包暂用 debug 签名，便于本地安装自测。
  - 真正上架前应切换为独立 release keystore：将 keystore 放在仓库外（如 `~/.gradle/keystore/renovation-release.jks`），密码通过 `gradle.properties` 的环境变量注入，**不硬编码、不进 git**。
  - 具体字段名与安全约定见 `keystore/README.md`。

## 本地数据与历史 Web 版的关系

- 知识数据（流程、避坑、风格、价格、验收清单、百科）在 Android 端**独立维护一份**：`assets/knowledge.json` + `assets/prices.json`，启动时一次性读入内存缓存（`data/knowledge/KnowledgeCache.kt`）。
- 与 Web 版 `js/data/knowledge.js` / `js/data/prices.js` **不同步**；Web 数据已冻结，后续知识更新只在 Android 端维护。现有字段结构源于历史 Web `window.DATA` / `window.PRICES`（剔除 IIFE / 函数）。结构定义见 `data/knowledge/KnowledgeJson.kt` / `PricesJson.kt`。
- 用户数据（任务、阶段进度、预算、支出、联系人、笔记、房屋信息）存 Room；现有 JSON 备份的部分字段命名参考历史 Web `Store.state`，仅便于语义对照，不代表格式兼容或后续必须保持对齐（见 `data/repo/ImportExportRepository.kt`）。
- 当前不支持 Web JSON 备份 → Android 自动迁移；旧版备份请单独留档，用户需在 Android 端重新完成首次设置向导。

## 测试

- **单元测试**：`gradlew.bat :app:testDebugUnitTest`。覆盖 Room DAO 关键聚合（首页三段分组 / 阶段完成度 / 分类超支 / CSV 扁平视图）、ViewModel 逻辑（首次设置 / 首页 / 预算）、纯函数（CSV 拼装、JSON 导入导出）。
- **Instrumented / Compose UI**：`gradlew.bat :app:connectedDebugAndroidTest`，覆盖首次设置、首页三段分组、阶段勾选、预算录入、CSV 导出、JSON 导入/导出关键交互。