# 装修管家 · Android 装修全流程助手

一个陪你从收房走到入住的装修助手：流程打卡、预算记账、避坑提醒、验收清单，核心功能离线可用。

> **项目状态（2026-09-05）：网页版已封存，后续仅专注 Android App 开发。**
>
> 新功能、体验优化和问题修复均围绕 `android/` 原生应用推进，不再双线维护或要求 Web / Android 功能对齐。网页版源码原位保留，仅供历史参考和存量数据备份，不再新增功能或进行常规维护。

## 维护范围

| 端 | 状态 | 说明 |
|---|---|---|
| **Android App** | **持续开发，唯一后续开发端** | 工程位于 [`android/`](android/)，原生 Kotlin + Jetpack Compose |
| 网页版 | **已封存** | 根目录 `index.html`、`js/`、`css/` 及相关启动脚本保留，不再作为开发主线 |

## 功能概览（Android）

- **首次设置向导**：填写房屋信息、装修方式与预算，生成装修计划
- **首页**：查看装修进度、待办任务与预算概况
- **流程**：按装修阶段管理任务、查看避坑要点与验收清单
- **预算**：管理分类预算和实际支出，查看超支情况，导出支出 CSV
- **指南**：查阅装修知识、避坑指南、验收清单等离线资料
- **我的**：管理房屋信息、联系人和笔记，导入 / 导出 JSON 备份

## 使用方法（Android）

### 构建与安装

- 运行设备：Android 7.0 及以上（minSdk 24）
- 开发环境：Android Studio、与工程兼容的 Gradle JDK，以及 Android SDK（当前 compileSdk 37；版本以 [`android/gradle/libs.versions.toml`](android/gradle/libs.versions.toml) 为准）
- 用 Android Studio 打开 `android/` 目录，完成 Gradle 同步后，可直接构建并运行到真机或模拟器

也可在仓库根目录打开 PowerShell，执行：

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

调试 APK 输出到 `android/app/build/outputs/apk/debug/app-debug.apk`（相对仓库根目录），安装到 Android 设备即可使用。Linux / macOS 下使用 `./gradlew` 替代 `.\gradlew.bat`。

完整的构建、调试、测试与签名说明见 [Android 开发说明](android/README.md)。当前 release 构建暂用 debug 签名，仅用于本地自测；正式分发前需配置独立的 release 签名。

### 数据说明

- 用户数据保存在设备本地的 Room（SQLite）数据库，无后端、无登录、无云同步
- 装修知识和价格参考随 App 内置，离线可用，不强依赖 Google Play / GMS
- 支持 Android 端 JSON 备份 / 恢复，以及支出明细 CSV 导出；卸载应用或换机前请先备份
- Web 浏览器数据与 Android 数据相互独立，**当前不支持将 Web JSON 备份自动迁移到 Android**；首次使用 Android 版仍需完成设置向导，旧版数据请单独留档

## 技术说明（Android）

- 原生 Kotlin + Jetpack Compose + Material 3
- Navigation Compose 管理五个 Tab 与首次设置向导
- Room 管理用户数据，`assets/*.json` 保存只读知识数据
- Gradle Wrapper 构建；依赖和 SDK 版本统一以 [`android/gradle/libs.versions.toml`](android/gradle/libs.versions.toml) 为准
- Android 知识数据独立维护，后续变更不再要求同步到已封存的网页版

## 项目结构

```text
renovation/
├── android/                  # 唯一持续开发的 Android App 工程
│   ├── README.md             # Android 构建、调试、测试与签名说明
│   ├── app/
│   │   ├── src/main/         # Kotlin / Compose 源码、资源与内置知识数据
│   │   ├── src/test/         # JVM 单元测试
│   │   └── src/androidTest/  # 设备 / Compose UI 测试
│   ├── gradle/               # Gradle Wrapper 配置与版本目录
│   └── gradlew.bat           # Windows 构建入口
├── .trellis/                 # 开发规范、任务与会话记录
├── index.html                # 以下为已封存的网页版文件
├── css/                      # 历史 Web 样式
├── js/                       # 历史 Web 逻辑与知识数据
├── server.js                 # 历史局域网静态服务器
├── open-app.bat              # 历史浏览器启动入口
└── phone-server.bat          # 历史局域网访问启动脚本
```

## 开发说明

本项目使用 Trellis 任务流管理开发进度：开发规范见 `.trellis/spec/`，任务记录见 `.trellis/tasks/`，配合 AI 协作完成迭代。

- 后续需求默认只面向 Android App，优先阅读 [Android 开发规范](.trellis/spec/android/index.md) 与 [Android 目录结构](.trellis/spec/android/directory-structure.md)
- Android 的使用与工程细节见 [`android/README.md`](android/README.md)
- `.trellis/spec/frontend/` 与 `.trellis/spec/backend/` 保留为已封存网页版及静态服务器的历史说明，不再作为新功能开发入口

## 网页版（已封存，仅供历史参考）

以下方式仅用于查看旧版或导出存量数据，不代表恢复网页版开发，也不是 Android App 的运行方式。

- **本地查看**：双击 `index.html`，或运行 `open-app.bat`，用浏览器打开旧版
- **手机局域网查看**：安装 Node.js 后运行 `phone-server.bat`，手机连接同一 Wi-Fi，并在浏览器打开终端打印的局域网地址（如 `http://192.168.x.x:8787`）；关闭终端即停止服务
- **历史技术栈**：vanilla JS / HTML / CSS，零依赖、零构建的静态 Web 应用；`server.js` 仅提供静态文件，不存储用户数据
- **历史数据**：保存在各设备、各浏览器的 localStorage 中，可用旧版导出 JSON 备份或 CSV 支出明细；清理浏览器数据前请先备份，不要使用无痕窗口长期保存数据

## 更新日志

本项目不维护单独的 CHANGELOG 文件，更新记录以 git 提交历史为准：

```bash
git log --oneline
```

提交信息采用「中文类型前缀 + 中文描述」格式，类型包括 `新增` / `修复` / `文档` / `重构` / `配置` / `性能` / `测试`。示例：`新增: 首次设置向导`、`修复: 预算超支计算`、`文档: 明确网页版封存与 Android 开发方向`。详见 [项目开发约定](.trellis/spec/project-conventions.md)。
