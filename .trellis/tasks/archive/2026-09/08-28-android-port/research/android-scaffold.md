# Research: Android 工程脚手架（Compose 单 module）

- **Query**: 为「装修管家 Android 移植」任务选定现代 Kotlin + Jetpack Compose 单 module 工程的脚手架；覆盖 Gradle/Kotlin/Compose BOM 版本、minSdk/targetSdk/compileSdk 选型、依赖列表、签名与构建产物、调试命令、Material You 接入。
- **Scope**: external（大部分）+ internal（与现有 Web SPA 工程对比）
- **Date**: 2026-08-29
- **Task**: `.trellis/tasks/08-28-android-port`（PRD `prd.md`，D1 = 原生 Kotlin + Compose 重写，单 module / MVP）

---

## 1. 推荐版本组合（2026-08-29 现状）

> 数据来源：Google Maven (`dl.google.com/android/maven2/...`)、Gradle services (`services.gradle.org/versions/current`)、Kotlin 官方发布页、AGP 9.3.0 发布说明（android-docs.cn 镜像）。

| 组件 | 推荐版本 | 备注 / 数据来源 |
|---|---|---|
| **Android Gradle Plugin (AGP)** | **9.3.2**（stable） | Google Maven 截至 2026-08-27 的最新 stable；9.4.0-rc02 / 9.5.0-alpha03 在管线中。`dl.google.com/.../maven-metadata.xml` |
| **Gradle Wrapper** | **9.5.0**（AGP 9.3 的默认值与最低值） | AGP 9.3.0 release notes 明确「Gradle 9.5.0 / 9.5.0」。`android-docs.cn/build/releases/agp-9-3-0-release-notes` |
| **Kotlin (JVM/AGP)** | **2.4.10**（stable，2026-07-14） | Kotlin release page：2.4.10 bug-fix 2026-07-14；2.4.0 language 2026-06-03；2.5.0 / 2.4.20 排期 2026-09/12。`kotlinlang.org/docs/releases.html` |
| **Compose BOM** | **2026.08.00**（2026-08-12 发布） | `dl.google.com/.../androidx/compose/compose-bom/maven-metadata.xml` latest = `2026.08.00` |
| **Material 3** | 由 BOM 解析（建议先看 BOM 映射表确认） | `androidx.compose.material3:material3` |
| **Activity Compose** | **1.13.0**（stable） | `dl.google.com/.../androidx/activity/activity-compose/maven-metadata.xml` |
| **Lifecycle ViewModel Compose** | **2.11.0**（stable） | `dl.google.com/.../androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml` |
| **Navigation Compose** | **2.10.0**（stable，2026-08-26） | `dl.google.com/.../androidx/navigation/navigation-compose/maven-metadata.xml` |
| **Room** | **2.8.4**（stable） | `dl.google.com/.../androidx/room/room-runtime/maven-metadata.xml` |
| **kotlinx-serialization-json** | **1.11.0**（stable） | Maven Central |
| **kotlinx-datetime** | **0.8.0**（stable，2026-05-07） | Maven Central（另有 `0.8.0-0.6.x-compat` 兼容包） |
| **kotlinx-coroutines-android** | **1.11.0**（stable，2026-05-08） | Maven Central |
| **JDK** | **17**（AGP 9.x 要求与默认） | AGP 9.3 release notes: `JDK 17 / 17` |
| **SDK Build Tools** | **36.0.0** | AGP 9.3 release notes: `SDK Build Tools 36.0.0 / 36.0.0` |
| **minSdk** | **24**（Android 7.0 Nougat） | PRD 临时假设：`minSdk ≥ 24`，覆盖 7.0+ |
| **targetSdk** | **35**（Android 15） | 与 2025/2026 主流 Play 商店「新应用需 targetSdk 最近两个版本以内」政策一致 |
| **compileSdk** | **36**（Android 16） | AGP 9.3 支持最高 API 37；用 36 与最新 Android 16 SDK 保持一致 |

### 1.1 关于「版本取最新 vs 跟稳定」

- PRD D1 与 D4 已决定使用 Kotlin + Compose + Material You。AGP 9.x 已稳定 4+ 月（9.0.0 2025-07-31 → 9.3.2 2026-06），生态已经能正常 build。
- Compose BOM 月度发版（`2026.08.00` 是 2026-08-12），紧跟 Kotlin 2.4.x。**建议**采用 BOM 而不是写死每个 Compose artifact 的版本。
- 除非碰到具体兼容问题（见第 6 节风险），不必锁到更老的 AGP 8.x；AGP 9.x 已在主线。

---

## 2. 工程目录树示意（单 module，PRD D1 / D3）

```
renovation-android/                          ← 仓库根（与现有 Web SPA 平行的目录或子目录）
├── settings.gradle.kts                       ← pluginManagement / dependencyResolutionManagement
├── build.gradle.kts                          ← 根级（仅 plugins{} 块声明别名）
├── gradle.properties                         ← JVM 内存、AGP flags、Kotlin code style
├── gradle/
│   ├── libs.versions.toml                    ← 版本目录（Gradle 推荐的单一事实源）
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties         ← distributionUrl=…/gradle-9.5.0-bin.zip
├── gradlew                                   ← bash wrapper
├── gradlew.bat                               ← Windows / PowerShell wrapper
├── local.properties                          ← SDK 路径（不入 git；每台机器独立）
├── .gitignore
├── keystore/                                 ← 本地签名（不入 git；release keystore 仓库外保管）
│   ├── debug.keystore                        ← AGP 自动生成（首次 debug build 时）
│   └── release.keystore.example              ← 留模板，实际密钥不进库
├── docs/
│   └── SPEC.md                               ← 已存在的 `.trellis/spec/android/index.md` 引用入口
└── app/                                      ← 唯一 module（MVP 单 module）
    ├── build.gradle.kts                      ← android{} + dependencies{}
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml           ← <application> 入口、theme
        │   ├── java/com/example/renovation/  ← Kotlin 源码根（包名待定，建议 `com.renovation.guardian`）
        │   │   ├── MainActivity.kt
        │   │   ├── RenovationApp.kt          ← Application 子类（可选；用于启动时初始化）
        │   │   ├── ui/
        │   │   │   ├── theme/                ← Color.kt / Theme.kt / Type.kt
        │   │   │   ├── home/                 ← 首页（行动列表）
        │   │   │   ├── stages/               ← 14 阶段时间轴
        │   │   │   ├── budget/               ← 预算 + CSV 导出
        │   │   │   ├── guide/                ← 指南 5 子区
        │   │   │   ├── more/                 ← 我的页
        │   │   │   └── nav/                  ← NavHost
        │   │   ├── data/
        │   │   │   ├── db/                   ← Room AppDatabase / DAO / Entity
        │   │   │   ├── repo/                 ← Repository（任务、预算、联系人…）
        │   │   │   ├── knowledge/            ← 只读 assets JSON 加载（启动缓存）
        │   │   │   └── prefs/                ← DataStore（设置项）
        │   │   ├── domain/                   ← 业务模型与计算（逾期分组、超支判定）
        │   │   └── util/                     ← 格式化、日期、IO
        │   ├── assets/
        │   │   ├── knowledge.json            ← 流程/避坑/风格/验收/百科（PRD D2 单一事实源）
        │   │   └── prices.json               ← 档位/预算模板
        │   └── res/
        │       ├── values/
        │       │   ├── strings.xml
        │       │   ├── themes.xml            ← Theme.Renovation 父 Material3 主题
        │       │   └── colors.xml
        │       ├── values-night/
        │       │   └── themes.xml
        │       ├── mipmap-anydpi-v26/        ← adaptive launcher icon
        │       ├── drawable/
        │       └── xml/                       ← backup_rules / data_extraction_rules
        ├── test/                              ← 本地 JVM 单元测试
        │   └── java/com/example/renovation/
        │       ├── data/db/                  ← Room DAO 测试（in-memory DB）
        │       ├── domain/                   ← 业务逻辑测试
        │       └── util/
        └── androidTest/                       ← 插桩 / Compose UI 测试
            └── java/com/example/renovation/
                ├── ui/home/
                └── ui/stages/
```

> 注：上图为「目标结构」示意。MVP 阶段不必一步到位建齐；按业务需要渐进即可。

---

## 3. `app/build.gradle.kts` 关键依赖块示例

> ⚠️ 下面只是 **关键块示例**，不是可直接落地的完整文件（PR 阶段由 implement 子 agent 完成）。具体版本号见第 1 节表。

```kotlin
// app/build.gradle.kts —— 仅展示关键结构
plugins {
    alias(libs.plugins.android.application)    // com.android.application
    alias(libs.plugins.kotlin.android)         // org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)         // org.jetbrains.kotlin.plugin.compose  (Kotlin 2.0+ 必备)
    alias(libs.plugins.kotlin.serialization)   // org.jetbrains.kotlin.plugin.serialization
    alias(libs.plugins.ksp)                    // com.google.devtools.ksp（Room 编译用；非 KAPT）
}

android {
    namespace = "com.renovation.guardian"      // 替换为真实 applicationId
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.renovation.guardian"
        minSdk = libs.versions.minSdk.get().toInt()        // 24
        targetSdk = libs.versions.targetSdk.get().toInt()  // 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"     // 便于 debug/release 共存
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true             // R8 默认启用
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")   // 见第 5 节
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true                    // AGP 9 默认生成；按需开关
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ---- Compose BOM ----
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ---- Compose UI ----
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)              // M3
    implementation(libs.androidx.compose.material.icons.extended)

    // ---- Activity / Lifecycle / Navigation ----
    implementation(libs.androidx.activity.compose)               // ComponentActivity + setContent
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)     // viewModel() in Compose
    implementation(libs.androidx.navigation.compose)             // NavHost

    // ---- Room ----
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ---- DataStore（设置项，比 SharedPreferences 适合 Compose）----
    implementation(libs.androidx.datastore.preferences)

    // ---- Kotlinx ----
    implementation(libs.kotlinx.coroutines.android)              // 协程（Room 自带亦可，但显式更稳）
    implementation(libs.kotlinx.serialization.json)              // 知识 JSON / 备份 JSON
    implementation(libs.kotlinx.datetime)                        // 日期处理

    // ---- Debug tooling ----
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ---- Test ----
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)               // in-memory Room for JVM unit test
    testImplementation(libs.androidx.arch.core.core.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
```

> 关于 Compose 编译器：Kotlin 2.0 之后，Compose 编译器**独立成 Gradle 插件** `org.jetbrains.kotlin.plugin.compose`（已在 `plugins{}` 块声明），不再需要 `composeOptions { kotlinCompilerExtensionVersion = ... }`。

### 3.1 `gradle/libs.versions.toml` 关键节选

```toml
[versions]
agp                 = "9.3.2"
kotlin              = "2.4.10"
ksp                 = "2.4.10-1.0.30"        # 建议查阅 KSP releases 时的实际对应
composeBom          = "2026.08.00"
activityCompose     = "1.13.0"
lifecycle           = "2.11.0"
navigation          = "2.10.0"
room                = "2.8.4"
coroutines          = "1.11.0"
serializationJson   = "1.11.0"
datetime            = "0.8.0"
datastore           = "1.2.0"                # 建议查 androidx datastore 实际最新
junit4              = "4.13.2"
androidxJunit       = "1.3.0"
espresso            = "3.7.0"

minSdk              = "24"
targetSdk           = "35"
compileSdk          = "36"

[libraries]
androidx-compose-bom                 = { module = "androidx.compose:compose-bom",                version.ref = "composeBom" }
androidx-compose-ui                  = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics         = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling          = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview  = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-test-manifest    = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-compose-ui-test-junit4      = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-material3           = { module = "androidx.compose.material3:material3" }
androidx-compose-material-icons-ext  = { module = "androidx.compose.material:material-icons-extended" }

androidx-activity-compose            = { module = "androidx.activity:activity-compose",          version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx       = { module = "androidx.lifecycle:lifecycle-runtime-ktx",   version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose          = { module = "androidx.navigation:navigation-compose",     version.ref = "navigation" }

androidx-room-runtime                = { module = "androidx.room:room-runtime",                 version.ref = "room" }
androidx-room-ktx                    = { module = "androidx.room:room-ktx",                     version.ref = "room" }
androidx-room-compiler               = { module = "androidx.room:room-compiler",                version.ref = "room" }
androidx-room-testing                = { module = "androidx.room:room-testing",                 version.ref = "room" }

androidx-datastore-preferences       = { module = "androidx.datastore:datastore-preferences",  version.ref = "datastore" }

kotlinx-coroutines-android           = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json           = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serializationJson" }
kotlinx-datetime                     = { module = "org.jetbrains.kotlinx:kotlinx-datetime",      version.ref = "datetime" }
kotlinx-coroutines-test              = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

junit                                = { module = "junit:junit",                                version.ref = "junit4" }
androidx-test-ext-junit              = { module = "androidx.test.ext:junit",                    version.ref = "androidxJunit" }
androidx-espresso-core               = { module = "androidx.test.espresso:espresso-core",       version.ref = "espresso" }

[plugins]
android-application                  = { id = "com.android.application",                        version.ref = "agp" }
kotlin-android                       = { id = "org.jetbrains.kotlin.android",                  version.ref = "kotlin" }
kotlin-compose                       = { id = "org.jetbrains.kotlin.plugin.compose",           version.ref = "kotlin" }
kotlin-serialization                 = { id = "org.jetbrains.kotlin.plugin.serialization",     version.ref = "kotlin" }
ksp                                  = { id = "com.google.devtools.ksp",                        version.ref = "ksp" }
```

> KSP 的版本号需与 Kotlin 严格对齐（Kotlin 2.4.x ↔ KSP 2.4.10-x.x.x），**建议查阅 KSP releases 时再次确认**。

---

## 4. minSdk / targetSdk / compileSdk 选型

### 4.1 取值与理由

| 项 | 推荐 | 理由 |
|---|---|---|
| **minSdk = 24** | Android 7.0 Nougat（2016） | PRD 临时假设已写「≥ 24」。24 是现代 Compose 库的硬性下限（很多 androidx 库已不兼容 < 24）。覆盖国内主流用户。 |
| **targetSdk = 35** | Android 15 | Google Play 2025 起要求「新应用 / 应用更新」以最近 1–2 个 Android 版本为目标；targetSdk 35 仍在 Google Play 的「合规窗口」内（2026 年中后期 Play 推 36 切换）。 |
| **compileSdk = 36** | Android 16 | AGP 9.3.0 支持最高 API 37；用 36 与最新 SDK 对齐。**注意**：compileSdk 高于 targetSdk 合法；编译期就能用 36 的 API。 |

### 4.2 各版本对 App 的实际影响

- **Android 13 (API 33)**
  - 引入 `POST_NOTIFICATIONS` 运行时权限（`Manifest.permission.POST_NOTIFICATIONS`）。**MVP 不用通知**，但需在 `AndroidManifest.xml` 里 *不声明*，避免 targetSdk 升级后被系统要求弹窗。
  - 引入 Photo Picker / 细化的媒体权限；MVP 不涉及选图，可不引入。

- **Android 14 (API 34)**
  - `targetSdk 34+` 起，部分隐式 intent 要求显式 `setPackage` 或 `FLAG_ACTIVITY_REQUIRE_NON_BROWSER`；MVP 不做 Intent.ACTION_SEND 之外的复杂意图分发。
  - 隐式 / PendingIntent mutability 行为变化——MVP 不挂 PendingIntent，影响小。
  - 不强制 edge-to-edge。

- **Android 15 (API 35) — targetSdk 35 时启用**
  - **强制 edge-to-edge**：以 API 35 为目标的 App 默认绘制到状态栏 / 导航栏下；需要调用 `enableEdgeToEdge()` 或正确处理 `WindowInsets`。MVP 是单列布局，**应**主动 `enableEdgeToEdge()`（见第 6 节）。
  - 已停止支持 Android 6.0 及以下（与 minSdk 24 无冲突）。
  - 前台服务类型需声明（MVP 无前台服务，影响小）。

- **Android 16 (API 36) — compileSdk 36**
  - targetSdk 36 才会触发的行为变更（与 MVP 短期无关，但需要知道）：
    - 自适应必需性 / Predictive Back 全默认开启。
    - 通知「以进度为中心」的新模式。
    - 部分隐式广播进一步收紧。
  - 实际生效要等 targetSdk 升到 36 之后。

### 4.3 PRD 临时假设与决策记录

- PRD D1 假设「minSdk ≥ 24」已合理；本文档确认 **minSdk 24 / targetSdk 35 / compileSdk 36** 作为 MVP 默认。
- 切到 targetSdk 36（Android 16）建议在 2026 末或后续迭代中再升，避免一次性承担 Android 15/16 全部行为变更。

---

## 5. 签名与构建产物

### 5.1 Debug keystore

- AGP 默认在 `~/.android/debug.keystore` 自动生成，无需手动创建。
- Gradle 8+ 起 debug build 也会用这个 keystore；口令固定为 `android`。
- **`app/build.gradle.kts` 默认 `signingConfig = signingConfigs.getByName("debug")` 即可。**

### 5.2 Release keystore（本地约定，不入 git）

```bash
# 一次性创建（PowerShell 等价用 keytool -genkeypair）
keytool -genkeypair -v \
  -keystore keystore/release.keystore \
  -alias renovation-release \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <从密码管理器取> -keypass <同上> \
  -dname "CN=Renovation Guardian,O=Personal,C=CN"
```

- 把 `keystore/*.keystore` 加进 `.gitignore`。
- **仓库内**只留 `keystore/release.keystore.example`（含字段名 + dummy 密码）作为模板，并加 README 提示"真密钥通过密码管理器同步给开发者本地"。
- 在 `~/.gradle/gradle.properties` 或 CI secrets 注入：

  ```properties
  RENOVATION_STORE_FILE=keystore/release.keystore
  RENOVATION_STORE_PASSWORD=...
  RENOVATION_KEY_ALIAS=renovation-release
  RENOVATION_KEY_PASSWORD=...
  ```

- `app/build.gradle.kts` 读环境变量（**不要**把密码硬编码到 `build.gradle.kts`）：

  ```kotlin
  signingConfigs {
      create("release") {
          storeFile = file(providers.gradleProperty("RENOVATION_STORE_FILE").get())
          storePassword = providers.gradleProperty("RENOVATION_STORE_PASSWORD").get()
          keyAlias = providers.gradleProperty("RENOVATION_KEY_ALIAS").get()
          keyPassword = providers.gradleProperty("RENOVATION_KEY_PASSWORD").get()
      }
  }
  ```

### 5.3 APK / AAB 输出路径

- 单 module 默认输出在 `app/build/outputs/apk/<variant>/` 与 `app/build/outputs/bundle/<variant>/`。
- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release AAB（推荐用于分发）：`app/build/outputs/bundle/release/app-release.aab`
- Release APK（如果走本地分发）：`app/build/outputs/apk/release/app-release.apk`

### 5.4 AAB 与 Play 上架（**本任务 Out of Scope，提一句**）

- Google Play 自 2021 年 8 月起要求新应用以 **AAB** 格式上架。
- **本任务**（MVP 本地构建 / 局域网分发 / 后续可能的国内应用市场）**不一定需要 AAB**；debug + release APK 已可覆盖开发与自测。
- 真正上 Play 时，需要：Play Console 帐号、签名密钥（推荐用 Play App Signing，**别**只靠本地 release.keystore）、隐私政策、目标 API 等级合规说明。PRD 已将「应用市场上架所需的合规材料」明确列入 Out of Scope。

---

## 6. 调试与常用命令

> Windows PowerShell 与 bash 行为差异：AGP / Gradle 的 wrapper 都做了平台适配，命令名与输出一致；仅路径分隔符不同。

| 用途 | bash / zsh (macOS / Linux / Git Bash) | PowerShell (Windows) |
|---|---|---|
| **构建 Debug APK** | `./gradlew :app:assembleDebug` | `.\gradlew.ps1 :app:assembleDebug`（或 `gradlew.bat :app:assembleDebug`） |
| **构建 Release APK** | `./gradlew :app:assembleRelease` | `.\gradlew.ps1 :app:assembleRelease` |
| **构建 Release AAB** | `./gradlew :app:bundleRelease` | `.\gradlew.ps1 :app:bundleRelease` |
| **安装 Debug 到连接的设备** | `./gradlew :app:installDebug` | `.\gradlew.ps1 :app:installDebug` |
| **本地单元测试 (JVM)** | `./gradlew :app:test` 或 `./gradlew :app:testDebugUnitTest` | `.\gradlew.ps1 :app:test` |
| **插桩 / Compose UI 测试** | `./gradlew :app:connectedDebugAndroidTest` | `.\gradlew.ps1 :app:connectedDebugAndroidTest` |
| **Lint** | `./gradlew :app:lintDebug` 或 `./gradlew :app:lintRelease` | `.\gradlew.ps1 :app:lintDebug` |
| **清理** | `./gradlew clean` | `.\gradlew.ps1 clean` |
| **查看可用任务** | `./gradlew :app:tasks --all` | `.\gradlew.ps1 :app:tasks --all` |
| **依赖树** | `./gradlew :app:dependencies` | `.\gradlew.ps1 :app:dependencies` |
| **在 Windows 上显式 wrapper jar 路径** | n/a | `.\gradlew.bat` 等价于 `.\gradlew.ps1`，视项目模板而定 |

### 6.1 PowerShell 注意事项

- Windows 默认 PowerShell 5.1 不识别 `&&`，需用 `;` 或 `cmd1; if ($?) { cmd2 }`。
- Gradle daemon 缓存路径在 Windows 上是 `C:\Users\<user>\.gradle\caches\...`；遇到奇怪问题时 `.\gradlew.ps1 --stop` + 删除 `.gradle\caches\transforms-*` 经常能恢复。
- 长路径问题：把项目放在短路径（如 `C:\code\renovation-android`）下，避免 Windows MAX_PATH 限制。

### 6.2 常用 IDE 替代

- Android Studio Ladybug / Meerkat 系列都支持 AGP 9.x 与 Kotlin 2.4.x；具体版本以 `android-docs.cn/studio` 公布为准。
- Compose 预览、Layout Inspector、Database Inspector（看 Room）都是 Studio 自带。

---

## 7. Material You（dynamic color）接入示例

> PRD D4 已明确「MVP 仅引入 Material You 动态取色」。下面的接入是 **MainActivity 端** 的入口示例。具体颜色 token 的覆写（把品牌色「米色 #f6f3ee」融入 light scheme）建议在另一个独立 spec / 任务里做。

### 7.1 关键依赖

- `androidx.compose.material3:material3`（已在 BOM 内）
- `androidx.activity:activity-compose`（含 `ComponentActivity.enableEdgeToEdge()` 与 `setContent`）
- Android 12 (API 31) 才有 `dynamicLightColorScheme(context) / dynamicDarkColorScheme(context)`；低版本自动回退到静态 `lightColorScheme(...)` / `darkColorScheme(...)`。

### 7.2 `MainActivity.kt` 接入点（示例代码，非产出文件）

```kotlin
package com.renovation.guardian

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.renovation.guardian.ui.theme.RenovationTheme  // 见 7.3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                  // Android 15 (targetSdk 35) 要求默认 edge-to-edge
        setContent {
            RenovationTheme {
                // AppRoot / NavHost
            }
        }
    }
}

@Composable
fun RenovationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,           // 开关位；用户在「我的」页可关
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12 (API 31) 起支持 dynamic color
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> darkColorScheme()      // 回退静态暗色
        else      -> lightColorScheme()     // 回退静态亮色（这里把品牌「米色」刷进 primary 等槽位）
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = RenovationTypography,    // 后续在 ui/theme/Type.kt 定义
        // shapes    = RenovationShapes,
        content    = content
    )
}
```

### 7.3 与 Web 版米色品牌的衔接策略

- 静态回退（`lightColorScheme()`）应当用 PRD 已知的 Web 主色 `#f6f3ee` 作为 surface / background 的种子；具体刷法在独立的 design 任务里做：
  - 用 [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/) 或 `material-color-utilities` 把 `#f6f3ee` 当 source color 算一整套 M3 token。
  - `primary / secondary / tertiary` 由这套 token 决定；`onPrimary` 等内容色由 M3 算法自动算。
- `dynamicColor = true` 是默认；用户的「我的」页提供开关 `Use dynamic color`，存到 DataStore（`androidx.datastore.preferences`）。
- Android 12 以下不弹任何「取色」UI；只是 `dynamicColor && SDK_INT >= S` 自动为 false。
- 暗色模式：用 `isSystemInDarkTheme()` 跟随系统；M3 token 同时提供 light / dark 两套。

### 7.4 风险点

- 动态色 + 极浅米色品牌同时存在时，**对比度**可能不过 WCAG：例如 wallpaper 是深红时，dynamic primary 可能非常饱和；建议关键操作（按钮）仍用 `colorScheme.primary` 而非「品牌米色」直接做背景。
- API 31 之后才支持；测试矩阵需包含 Android 7–11 的真机或模拟器（保持静态色板回退可读）。

---

## 8. 风险与注意事项

### 8.1 AGP ↔ Gradle ↔ Kotlin 兼容性矩阵

- **AGP 9.3.2 ↔ Gradle 9.5.0+ ↔ Kotlin 2.4.x** 是当前官方推荐组合（已交叉验证：AGP 9.3 release notes 写明 Gradle 9.5.0，Kotlin 2.4.10 是当前 stable）。
- 升级路径：Kotlin 2.4 → 2.5 预计 2026-09 / 2026-12（Kotlin release 页公告）。届时建议复查 KSP 与 Compose Compiler 插件的对应版本。
- 降级风险：若用 AGP 9.3 遇到某种第三方插件不兼容，回退到 AGP 8.13.x（最后 8.x 稳定线）也是可行，但 Gradle 要降回 8.x。

### 8.2 namespace 与 applicationId

- **AGP 8.0+** 起 `namespace`（`build.gradle.kts` 里的 `android.namespace`）成为必填；与 manifest 根节点的 `package` 属性互斥。
- `namespace` 决定 R 类与 BuildConfig 的包名。
- `applicationId` 才是设备识别 App 的 ID（上传商店的标识）。debug 用 `applicationIdSuffix = ".debug"` 即可与 release 共存。

### 8.3 Compose Compiler 插件（Kotlin 2.0+ 强制）

- Kotlin 2.0 起 Compose 编译器**不是** `kotlinCompilerExtensionVersion`，而是 `org.jetbrains.kotlin.plugin.compose` 这个 **Gradle plugin**。
- 如果用 `kotlin-compose` plugin alias，Kotlin 与 Compose Compiler 版本由 Kotlin 一致管控；不要手动 `composeOptions { kotlinCompilerExtensionVersion = "..." }`（AGP 9.x 已不再允许）。

### 8.4 KSP vs KAPT

- Room 官方推荐 KSP（更快）；KSP 版本必须与 Kotlin 严格对齐。
- 上述 `libs.versions.toml` 里给的 `ksp = "2.4.10-1.0.30"` 是占位示例；**建议查阅 [KSP releases](https://github.com/google/ksp/releases) 时的实际对应号**（同一 Kotlin 版本通常有多个 KSP 修订）。

### 8.5 targetSdk 35 的 edge-to-edge 默认行为

- Android 15 (API 35) targetSdk 时，`enableEdgeToEdge()` 成为推荐；不调用也会被强制 edge-to-edge，但状态栏 / 导航栏区域要由 App 自己处理。
- `enableEdgeToEdge()` 是 `androidx.activity:activity-ktx / activity-compose` 1.8+ 引入的扩展，1.13.0 已稳定。
- 用 `Scaffold` + `WindowInsets.safeDrawing` / `Modifier.windowInsetsPadding(WindowInsets.systemBars)` 即可让内容不被遮挡。

### 8.6 离线 / 国内网络

- Maven Central 与 Google Maven 在国内偶有抖动；可考虑配 `dependencyResolutionManagement` 的 `repositories { ... maven("https://maven.aliyun.com/repository/public") ... }` 作为镜像。
- 建议在 `settings.gradle.kts` 把 `mavenCentral()` 与 `google()` 同时声明，本地开发者可加阿里云镜像，CI 走默认源。

### 8.7 R8 / 资源缩减

- `isMinifyEnabled = true` + `isShrinkResources = true` 是 release 默认；Room、Kotlinx Serialization 等用了反射的库需要保留规则，AGP 9.x 已有大量默认 `consumer-proguard`，但仍建议测试 release 包是否能正常启动、DAO 是否能查表。
- 调试 release 资源丢失问题：`./gradlew :app:assembleRelease -PenableR8.fullMode=true`（fullMode 严格度更高，是 AGP 9 默认）。

### 8.8 Compose BOM 升级副作用

- BOM 月度发版，BOM 升级会带动 Compose UI / Material3 / 工具等子库一起升；通常兼容，但 navigation / lifecycle 各自有自己的版本线。
- 升级时建议分次：先升 BOM，再分别试 navigation / lifecycle / activity-compose；不要一波全升。

### 8.9 其它「建议查阅时再次确认」

- **Android Gradle Plugin 与 NDK 版本**：本任务不用 NDK（无原生代码），但若后续引入第三方 .so（如 sqlite-vec、onnxruntime），需要看 AGP 9.3 对 NDK 28.2.13676358 的默认绑定。
- **AndroidX BOM**：本任务未引入 androidx-bom（依赖列表已逐个写版本）；如需统一管理，可考虑加 `androidx-bom`，但 Compose BOM 与 androidx-bom 互不冲突。
- **JDK 17 → 21**：AGP 9.3 默认 17，JDK 21 也可工作；若团队已全部升级到 JDK 21，保持 17 即可（Kotlin compile target 也是 17）。
- **Material Icons Extended** 版本：BOM 内置，但包体较大（~6MB），可考虑按需引入 `material-icons-core` + 单独用到的 `material-icons-extended-*` 子集。
- **kotlinx-datetime 0.8.0 的协变性**：0.8 与 0.6 API 有 breaking change（如 `Instant` 现在是 `kotlin.time.Instant` 包装）。从 0.6 升到 0.8 时需要替换 import 与 API。

---

## 9. 与 PRD D1–D5 / 后续研究主题的关联

- 本研究是 PRD「Research Topics」第 1 项「Android 工程脚手架」，是后续所有实现工作的前置。
- 与 **主题 2「Room schema 规划」**的衔接：依赖已就位（Room 2.8.4 + KSP）；schema 落库与迁移测试可以立即开始。
- 与 **主题 3「Web JSON 形态分析」**的衔接：Android 端的 `assets/knowledge.json` / `prices.json` 是「事实源」（PRD D2）；形态对照表由主题 3 给出，落到 Android 端的 `data/knowledge/` 加载器里。
- 与 **主题 4「Material You 取色方案」**的衔接：本研究的第 7 节是 **接入点示例**；具体的「米色 + 动态色 + 对比度合规」由主题 4 给出统一调色板。

---

## 10. 引用与外部参考

- AGP 9.3.0 发布说明（中文镜像，`android-docs.cn`）— 兼容性矩阵：
  https://android-docs.cn/build/releases/agp-9-3-0-release-notes
- AGP 9.2.0 发布说明 — 用于对照历史兼容线：
  https://android-docs.cn/build/releases/agp-9-2-0-release-notes
- Kotlin release process：
  https://kotlinlang.org/docs/releases.html
- Kotlin 2.4.10 GitHub Release：
  https://github.com/JetBrains/kotlin/releases/tag/v2.4.10
- Gradle 当前发布版本（services.gradle.org）：
  https://services.gradle.org/versions/current
- Gradle 全版本列表（`/versions/all`）：
  https://services.gradle.org/versions/all
- Android 16 主页（含 API 36）：
  https://android-docs.cn/about/versions/16
- Android 15 主页（含 API 35 / targetSdk 35 行为变更）：
  https://android-docs.cn/about/versions/15
- Compose Material 3 文档：
  https://android-docs.cn/develop/ui/compose/designsystems/material3
- Material Theme Builder（生成 Compose 调色板）：
  https://material-foundation.github.io/material-theme-builder/

### 10.1 实时版本元数据（2026-08-29 抓取）

- AGP：`https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml`
- Compose BOM：`https://dl.google.com/android/maven2/androidx/compose/compose-bom/maven-metadata.xml`
- activity-compose：`https://dl.google.com/android/maven2/androidx/activity/activity-compose/maven-metadata.xml`
- lifecycle-viewmodel-compose：`https://dl.google.com/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml`
- navigation-compose：`https://dl.google.com/android/maven2/androidx/navigation/navigation-compose/maven-metadata.xml`
- room-runtime：`https://dl.google.com/android/maven2/androidx/room/room-runtime/maven-metadata.xml`
- kotlinx-serialization-json：`https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml`
- kotlinx-datetime：`https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime/maven-metadata.xml`
- kotlinx-coroutines-android：`https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml`

---

## Caveats / Not Found

- **AGP 9.3 之外的 "最新版" 实时状态**：研究时确认 `9.3.2` 是 stable、`9.4.0-rc02` 与 `9.5.0-alpha03` 在管线。**正式写 PR 时建议再 fetch 一次** `https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml` 确认。
- **KSP 2.4.10 的对应版本号**：本研究给了占位 `"2.4.10-1.0.30"`，但 KSP 各小版本号是独立 release，**强烈建议 PR 实施时去 [KSP releases](https://github.com/google/ksp/releases) 拿与 Kotlin 2.4.10 对应的实际版本**。
- **DataStore Preferences 最新版本号**：文中给了 `"1.2.0"` 占位；**建议查阅 androidx datastore 当时的实际 release**。
- **`androidx.compose.material:material-icons-extended` 是否进 release 包**：依赖本身有 ~6MB 体积；MVP 可进，按需考虑按子集拆分。
- **targetSdk 36 的全部行为变更**：本任务 targetSdk 35，未完整覆盖 targetSdk 36 触发的行为；正式升级到 36 时建议再查 `https://android-docs.cn/about/versions/16/behavior-changes-16`。
- **google.cn / developer.android.com 直连时常失败**（2026-08-29 实测）：本研究通过 `android-docs.cn`（Google 官方中文镜像）与 `dl.google.com`（Google Maven 直连）获取数据；如需最新原文，建议通过 VPN / 镜像访问 `https://developer.android.com/build/releases/gradle-plugin`、`https://developer.android.com/about/versions/16/behavior-changes-16`。
