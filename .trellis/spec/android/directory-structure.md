# Directory Structure

## Overview

Android 版是单 module（`:app`）原生工程，入口 `MainActivity.kt`，无后端、无多 module 拆分。包根为 `com.renovation.guardian`。

## Directory Layout

```text
android/
├─ settings.gradle.kts          # 根工程名 renovation-android，include(":app")
├─ build.gradle.kts             # 根构建：只声明 plugin 别名（apply false）
├─ gradle/
│  └─ libs.versions.toml        # 版本目录（唯一事实源）
├─ gradle.properties            # JVM 参数 / AndroidX / Kotlin / KSP 开关
├─ gradlew / gradlew.bat        # Gradle wrapper
├─ keystore/                    # 签名约定（README.md，密钥不进 git）
└─ app/
   ├─ build.gradle.kts          # 应用构建：plugin 应用 / 依赖 / KSP / testOptions
   ├─ schemas/                  # Room 导出 schema（ksp room.schemaLocation）
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml
      │  ├─ assets/
      │  │  ├─ knowledge.json   # 只读知识数据
      │  │  └─ prices.json      # 只读价格数据
      │  ├─ java/com/renovation/guardian/
      │  │  ├─ MainActivity.kt  # 唯一 Activity，装 Compose 内容
      │  │  ├─ RenovationApp.kt # Application，持有 AppContainer / KnowledgeCache
      │  │  ├─ data/db/         # Room 实体 / DAO
      │  │  ├─ data/knowledge/  # JSON 模型 + KnowledgeCache + KnowledgeSeeder
      │  │  ├─ data/repo/       # 仓库层 + AppContainer
      │  │  ├─ ui/              # Compose Screen / ViewModel
      │  │  ├─ ui/theme/        # Color / Shape / Type / Theme
      │  │  └─ util/            # MoneyUtil / DateUtil / IdGen
      │  └─ res/                # 资源（mipmap / values / drawable / xml）
      ├─ test/                  # JVM 单元测试（Robolectric + MockK + Turbine）
      └─ androidTest/           # Instrumented / Compose UI 测试
```

## 分层边界

| 层 | 包 | 职责 | 可变性 |
|----|-----|------|--------|
| Entity/DAO | `data/db/` | Room 表结构、SQL 聚合、持久化 | 读写 |
| Knowledge | `data/knowledge/` | assets JSON 反序列化 + 内存缓存 + 种子写入 | 只读（除 Seeder） |
| Domain | `domain/quote/`、`domain/planner/` | 可序列化状态、纯计算、校验、规划规则 | 纯数据/纯函数 |
| Repository | `data/repo/` | 业务聚合、金额换算、导入导出、事务 | 读写 |
| ViewModel | `ui/*/` | 持有状态、暴露 Flow、发起业务调用 | 状态 |
| Screen | `ui/*/` | 纯 Compose 渲染 + 事件回调 | 无业务状态 |
| Theme | `ui/theme/` | Material You 色板 / 字体 / 形状 | 只读 |
| Util | `util/` | 金额 / 日期 / id 生成纯函数 | 无状态 |

## 依赖方向

- `ui/` 依赖 `domain/` 与 `data/repo/` → `data/db/`，数据层不得导入 UI。
- `domain/` 的状态/计算/校验仅依赖 Kotlin 与只读 knowledge DTO，不依赖 Compose、Room Repository；Repository 可调用 domain 校验。预算映射到分类的 UI 适配器仍位于 `ui/quote/engine/QuoteBudgetMapping.kt`。
- `data/repo/` 与 `ui/` 只能通过 `AppContainer` 拿到仓库实例，不直接 new `AppDatabase` / `KnowledgeCache`（见 `AppContainer.kt` 契约）。
- `data/knowledge/` 的 JSON 模型仅被 `KnowledgeCache` 与 `KnowledgeSeeder` 消费。
- `util/` 不依赖任何业务层，纯函数可被任意层引用。

## Naming

- 包后缀按层含义命名（`db` / `repo` / `ui` / `theme` / `util`）。
- 实体类以 `Entity` 结尾，DAO 以 `Dao` 结尾，仓库以 `Repository` 结尾，JSON 模型以 `Json` 结尾。
- 持久化 id 必须稳定：任务模板 id、预算分类 id、风格 id、验收清单 id、空间预设 id 已进入知识 JSON 与备份文件，不可随意改名。