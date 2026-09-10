# Quality Guidelines

## Overview

测试分三层：JVM 单元测试（Robolectric / MockK / Turbine）、Instrumented（Room 真机 / 模拟器）、Compose UI 测试。DoD 要求 lint / typecheck / 单测 / 仪器测试通过、APK 可签名构建。

## 测试分层

| 层 | 位置 | 工具 | 覆盖 |
|----|------|------|------|
| DAO 聚合 | `app/src/test/.../data/db/` | Robolectric + in-memory Room | 首页三段分组 / 阶段完成度 / 分类超支 / CSV 扁平视图 |
| Repository 导入导出 | `app/src/test/.../data/repo/` | Robolectric + in-memory Room | JSON 导出 / 导入原子性 |
| 纯函数 | `app/src/test/.../ui/budget/` | JUnit | `buildCsv` CSV 拼装 |
| ViewModel | `app/src/test/.../ui/*/` | MockK + Turbine（读 Flow）/ 真实 in-memory Room（写操作，见下） | 首次设置 / 首页 / 预算 / 我的 |
| DB Migration | `app/src/test/.../data/db/MigrationTest.kt` | Robolectric + 手工建 v1 库（见下） | v1→v2 迁移不丢数据 |
| Compose UI | `app/src/androidTest/` | compose-ui-test | 关键交互流 |

## 约定

- Room 单测用 `Room.inMemoryDatabaseBuilder` + `allowMainThreadQueries()`。
- ViewModel 测试注入 `mockk<AppContainer>(relaxed = true)` + `mockk<RenovationApp>`，stub `container.budgetRepo` 等 Flow 返回值。
- 纯函数测试不依赖 Android 运行时。

### ViewModel 写操作测试：禁用 MockK suspend 验证（Common Mistake）

**Symptom**：`coVerify { repo.xxx(any()) }` 报 "was not called"，无任何异常输出；同样的 `viewModelScope.launch { 普通代码 }` 却能立即执行。修改桩、换 dispatcher（`StandardTestDispatcher` / `UnconfinedTestDispatcher`）均无效。

**Cause**：MockK 的 suspend 桩在 `viewModelScope.launch` 协程内被调用时调用记录丢失（MockK 1.13.13 + coroutines 1.10.2 + lifecycle 2.10.0 组合实测）。读 Flow（`every { repo.observeAll() } returns flowOf(...)`）不受影响，只有 **suspend 桩的 coVerify** 会坏。

**Fix**：测 ViewModel 写操作（`viewModelScope.launch` 委托 repo 的方法）时，不走 MockK suspend 桩，改用**真实 in-memory Room 链路**：真实 `DefaultAppContainer` + 接口委托覆盖 `todayProvider` 为固定值，操作后查库断言：

```kotlin
val real = DefaultAppContainer(db, mockk<KnowledgeCache>(relaxed = true))
val container = object : AppContainer by real {
    override val todayProvider: () -> String = { "2026-09-04" }
}
val app = mockk<RenovationApp>(relaxed = true)
every { app.container } returns container
```

**同步约定**：Room executor 绑到 testScheduler，消除挂起 DAO 与测试线程的竞态：

```kotlin
val dispatcher = StandardTestDispatcher(testScheduler)
Dispatchers.setMain(dispatcher)
val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
    .allowMainThreadQueries()
    .setQueryExecutor(dispatcher.asExecutor())      // kotlinx.coroutines.asExecutor
    .setTransactionExecutor(dispatcher.asExecutor())
    .build()
// 每次 vm 写操作后 advanceUntilIdle() 再断言
```

参考实现：`app/src/test/.../ui/more/MoreViewModelTest.kt`。

### DB Migration 测试：禁用 MigrationTestHelper（Gotcha）

> **Warning**：AGP 9（9.3.2 实测）**没有 `mergeDebugUnitTestAssets` 任务**，test source set 的 assets（含 `sourceSets["test"].assets` 注册的目录）不会进 `apk-for-local-test.ap_`，`MigrationTestHelper` 永远报 "Cannot find the schema file"。**不要**尝试用 assets 路线。

替代方案：手工建 v1 库——从 `app/schemas/.../1.json` 读取 `entities[].createSql`（替换 `${TABLE_NAME}` 占位符）+ `indices[].createSql` + `setupQueries`（建 `room_master_table` 并写入 v1 identityHash），用 `FrameworkSQLiteOpenHelperFactory` + `SupportSQLiteOpenHelper.Callback(1)` 落真实 DB 文件，再 `Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()` 打开（Room 自动跑迁移并校验 schema）。参考实现：`app/src/test/.../data/db/MigrationTest.kt`。

### PowerShell 编辑含中文的文件（Gotcha）

> **Warning**：不要用 PowerShell `-replace` / `Set-Content` 批量修改含中文字符串的代码文件——多字节字符会被按行截断产生乱码（UTF-8 序列断裂），且报错位置与真实损坏位置不符。用 Edit/Write 工具。

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

## 本机工具与 Compose 同步（09-10 补充）

- Windows PowerShell 5.1 的默认管道编码是 ASCII。禁止把含中文的写文件脚本直接 `| python -`，否则可能得到问号。可用 `.NET File.WriteAllText(..., UTF8Encoding(false))` 生成临时脚本，再以显式 UTF-8 读取执行。
- 本机 Python 3.8 对含长中文行的源文件曾报 `Non-UTF-8`，而逐字节 UTF-8 解码正常。已验证的绕过方式：`exec(compile(path.read_text(encoding="utf-8"), str(path), "exec"))`；不要以忽略解码错误掩盖损坏。写后必须检查中文与 `git diff --check`。
- Robolectric Compose 等待 Room/VM 回调时，轮询原始 `mutableStateOf` 字段不等于驱动 UI 主循环。等待实际语义树条件（`fetchSemanticsNodes()`）或用 Compose 的 idle 同步；不能靠加长超时或删掉断言。
- 新增 `WorkflowComposeTest` 使用真实 Room 和真实目录，VM 注入只替换 Application 容器。截图是 JVM 渲染，不能替代设备上的系统文件选择器、分享和动态壁纸取色验证。
- `assembleDebugAndroidTest` 会暴露测试依赖的许可证资源冲突。使用 `packaging.resources.merges` 合并 `META-INF/LICENSE*` / `NOTICE*`，保留许可证，而不是删除测试或整个依赖。

- Windows JVM 下 AndroidX `FileProvider.SimplePathStrategy.belongsToRoot` 使用 `rootPath + '/'` 判断，而 Java canonicalPath 用反斜杠，因此有效 cache-path 也可能报无法找到根。不能为让单测通过放宽真实 provider 路径。JVM 文件操作测试用 ShadowContentResolver 注册流，验证写入/关闭/失败/取消；`AppNavigationUiTest.fileProviderAllowsReadingTheGeneratedImage` 在 Android 设备验证真实 URI。

- JUnit4 的 `@Test` 必须返回 void/Unit。不要写 `fun test() = runBlocking { ...assertIsDisplayed() }`：最后一个 Compose assertion 返回 SemanticsNodeInteraction，会让真实 AndroidJUnit4 runner 拒绝整个测试类。使用普通函数体，runBlocking 只包仓库调用，并用语义树等待异步 UI；APK 能打包不代表测试类可运行。
