# 封存网页版并聚焦 Android 的 README 更新

## 目标

明确网页版即日起封存，今后只专注 Android App 开发；项目入口文档应以 Android 为主，不再承诺双线维护或功能对齐。

## 已知事实

- 用户明确要求封存网页版、未来只开发 Android App，并更新 README。
- 根 README 目前以零构建 Web 应用为主，未提供 Android 主入口。
- android/README.md 与 Android 规范首页仍声明长期双线维护，需要同步消除冲突。
- Android 原生 Kotlin + Jetpack Compose 工程已存在，独立保存知识数据，用户数据采用 Room。

## 方案与范围

- 更新根 README 的定位、维护状态、功能概览、Android 使用与开发入口、目录结构。
- 保留网页版源码与历史启动/备份说明，明确停止功能迭代与常规维护，不再作为后续开发入口。
- 同步 Android README 与相关规范中的双线维护表述。
- 不删除、移动或修改 Web/Android 应用代码，不进行发布、迁移或远程仓库归档操作。

## 待确认问题

无阻塞问题；将“封存”落实为文档中的维护状态变更，保留历史代码可查。

## 验收标准

- [x] 根 README 首屏明确网页版封存、Android 为唯一后续开发端。
- [x] Android 的工程入口、构建命令、数据说明与仓库事实一致。
- [x] Web 使用方式仅出现在明确标记的历史参考部分。
- [x] Android README 及关联维护状态规范不再承诺双线开发或功能对齐。
- [x] 仅修改文档与任务记录；UTF-8 中文正常，Markdown 本地链接有效，git diff --check 通过。

## 完成标准

纯文档变更，检查差异、路径/命令与维护状态一致性；不运行与文档无关的 Android 构建或应用测试。不自动提交或推送。

## 技术参考

- README.md
- android/README.md
- .trellis/spec/project-conventions.md
- .trellis/spec/android/index.md
- .trellis/spec/android/build-signing.md
- android/app/build.gradle.kts
- android/gradle/libs.versions.toml

## 实施记录

- 根 README 已改为 Android 主入口；Web 启动与备份方式降级为封存参考。
- Android README 已取消双线维护要求，注明旧 Web 备份不自动迁移。
- 同步 Android 规范首页与知识数据规则，移除双端对齐要求；Web 前端/静态服务器规范首页标记为历史参考。
- 现有应用源码、构建配置、知识数据与启动脚本均未修改。

## 验证结果

- 已通过 6 份 Markdown 文档的 UTF-8、代码围栏与维护状态一致性检查。
- 已通过 29 个本地 Markdown 链接及文档所列源码/构建入口路径检查。
- git diff --check 已通过；仅有预期文档与当前任务记录变更，无应用代码变更。
- 构建命令、SDK 要求及签名提示已与现有 Gradle 配置核对；纯文档改动未执行 Android 构建、Lint 或应用测试。
- 已同步维护状态与知识数据开发约定，无新增 API、数据库或跨层契约。
- 文档工作与验证已完成；用户已确认提交方案，仅提交本任务文档及记录，不推送。提交后可执行 finish-work 封存任务并记录会话。
