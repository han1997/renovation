# Android 开发规范

> 本目录定义装修管家 **Android 版**（原生 Kotlin + Jetpack Compose）的开发规范。工程位于仓库根 `android/` 子目录，是项目唯一持续开发的应用端。自 2026-09-05 起 Web 版已封存，仅保留历史代码；后续不再双线维护或要求功能对齐。
>
> 本文档回应已完成的移植任务 `.trellis/tasks/08-28-android-port`，内容全部基于 `android/` 现有代码事实，不含虚构。

## Overview

- 技术栈：Kotlin 2.3.20 + Jetpack Compose（BOM 2026.08.00）+ Material 3 + Room 2.8.4 + Navigation Compose 2.9.5 + kotlinx-serialization 1.9.0 + kotlinx-datetime 0.7.1。
- 工程：单 module（`:app`），AGP 9.3.2，KSP 2.3.11，minSdk 24 / targetSdk 35 / compileSdk 37。
- 版本事实源：`android/gradle/libs.versions.toml`（唯一权威，含 Kotlin ↔ KSP 配对注释）。
- 数据分层：知识数据（只读）走 `assets/*.json` + 内存缓存；用户数据走 Room（SQLite）。
- 导航：`Navigation Compose`，五 Tab（home / stages / budget / guide / more）+ 顶层 `Onboarding` 首次设置向导。
- 全部文档/注释用中文。

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | 包结构、分层边界、模块契约 | Filled |
| [App Polish Contracts](./app-polish-contracts.md) | 金额/备份 v2/报价预算/规划/执行结果契约 | Filled |
| [Data Layer](./data-layer.md) | Room 表 / DAO、金额 cents 约定、知识数据 assets、种子写入 | Filled |
| [UI & Theme](./ui-theme.md) | Compose 界面分层、Material You 取色、静态回退色板 | Filled |
| [Type & Money](./type-money.md) | MoneyUtil 金额约定、JSON 数据类型安全、日期/id 约定 | Filled |
| [Build & Signing](./build-signing.md) | 构建/调试命令、签名约定、版本目录 | Filled |
| [Quality Guidelines](./quality-guidelines.md) | 测试分层、验证清单、DoD 对应 | Filled |

## Encoding Caution

与 Web 版一致：仓库中中文文案在终端输出承乱码形态，编辑时只改必要位置，避免对知识数据（`assets/knowledge.json` / `assets/prices.json`）做大段无关重写。新增文档使用 UTF-8 中文。

## Before Coding

Android 侧改动前至少阅读：

- 本目录 `index.md`。
- 与改动有关的专题 spec。
- 相关源码：`android/app/build.gradle.kts`、`android/gradle/libs.versions.toml`、对应 `data/db/*`、`data/repo/*`、`data/knowledge/*`、`ui/**/*`。