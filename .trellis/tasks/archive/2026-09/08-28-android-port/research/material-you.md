# Research: Material You 动态取色方案（Jetpack Compose 接入）

- **Query**：在 Jetpack Compose 中启用 Android 12+ 动态取色，并为 Android 7-11 提供静态色板回退；与 Web 版米色 `#f6f3ee` 品牌做融合。
- **Scope**：external（官方 Android Developers / codelab / 官方 API 引用 + Web 工程现状对照）
- **Date**：2026-08-29
- **任务**：`.trellis/tasks/08-28-android-port`

---

## 0. 背景与基线数据

### 0.1 Web 版品牌色板（`css/style.css:2-19`）

| Token | Hex | 角色 |
|---|---|---|
| `--bg` | `#f6f3ee` | 米色页面底（`theme-color` 也用这个） |
| `--card` | `#ffffff` | 卡片白底 |
| `--ink` | `#33312c` | 主文字（暖深灰） |
| `--muted` | `#8b8479` | 次文字 |
| `--line` | `#ece7de` | 分割线 / 弱边界 |
| `--brand` | `#cf6b45` | 品牌主色（陶土橙） |
| `--brand-dark` | `#b85a37` | 主色 hover / 强调 |
| `--brand-soft` | `#f9ebe4` | 主色低饱和填充 |
| `--green` | `#4f9d69` | 成功 / 完工 |
| `--red` | `#d15746` | 警示 / 超支 |
| `--amber` | `#c98f2b` | 待办 / 提醒 |
| `--blue` | `#5b84c4` | 信息 / 链接 |

`index.html:6`：`<meta name="theme-color" content="#f6f3ee">`，即系统顶栏 / 浏览器 chrome 跟随 `#f6f3ee`。

### 0.2 Web 版品牌定调

- 整体走"米色 + 暖橙 + 大量留白"的居家感；
- 圆角 `--radius: 16px`；
- 卡片白底浮在米色背景上。
- 没有任何"主色块大色面"，品牌色只用于按钮 / Tab 高亮 / 关键文字。

---

## 1. 关键 API 清单（`androidx.compose.material3`）

来源：[androidx.compose.material3 package summary](https://developer.android.google.cn/reference/kotlin/androidx/compose/material3/package-summary) 与 [Material Design 3 in Compose](https://developer.android.google.cn/develop/ui/compose/designsystems/material3) 官方文档。

| API | 签名 | minSdk / 注解 | 行为 |
|---|---|---|---|
| `dynamicLightColorScheme` | `fun dynamicLightColorScheme(context: Context): ColorScheme` | `@RequiresApi(31)`（= Android 12 = `S`），自 material3 1.0.0 | 从当前系统壁纸 + 主题派生的浅色 `ColorScheme` |
| `dynamicDarkColorScheme` | `fun dynamicDarkColorScheme(context: Context): ColorScheme` | `@RequiresApi(31)` | 从当前系统壁纸 + 主题派生的深色 `ColorScheme` |
| `lightColorScheme(...)` | `fun lightColorScheme(primary: Color, ..., scrim: Color): ColorScheme` | 无 | 自定义浅色静态色板；所有参数可缺省，回落到 Material 基线色 |
| `darkColorScheme(...)` | `fun darkColorScheme(primary: Color, ..., scrim: Color): ColorScheme` | 无 | 自定义深色静态色板 |
| `MaterialTheme(...)` | `MaterialTheme(colorScheme, typography, shapes, content)` | 无 | Compose Material 3 根包装，所有子组件通过 `MaterialTheme.colorScheme.*` 读色 |
| `isSystemInDarkTheme()` | `@Composable fun isSystemInDarkTheme(): Boolean` | 无 | 读取当前 UI Mode，返回是否暗色 |
| `LocalContext.current` | `CompositionLocal<Context>` | 无 | 在 Composable 内取 `Context` 给 `dynamicXxxColorScheme` |
| `staticCompositionLocalOf` / `compositionLocalOf` | — | 无 | 自定义 `ExtendedTheme` 注入额外色（如品牌主色硬约束） |

注意：`dynamicLightColorScheme` / `dynamicDarkColorScheme` 在 Android 12 之前的设备上**没有**静态回退版本，必须自己用 `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ... else LightColorScheme` 兜底；函数本身 `RequiresApi(31)`，所以**未分支的调用在 lint 阶段会报错**。

参考的官方 canonical 写法（来自 [Material 3 in Compose 文档](https://developer.android.google.cn/develop/ui/compose/designsystems/material3#dynamic-color-schemes)）：

```kotlin
// Dynamic color is available on Android 12+
val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val colors = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
    dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

（节选自 Google 官方 Material 3 主题页。）

---

## 2. 兼容策略：动态 vs 静态

### 2.1 推荐决策

**首版**采用"动态取色优先 + 静态兜底"两层：

| Android 版本 | 走哪个分支 | 备注 |
|---|---|---|
| 12+（API 31, S） | `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` | 由系统壁纸决定的色板；用户每次换壁纸都换皮肤 |
| 7.0–11（API 24–30） | 走 App 自带的"暖米色 + 陶土橙"静态色板（见 §3） | 与 Web 版视觉一致 |

触发条件：单纯 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`。这覆盖了：
- emulator 上 12+ 系统镜像；
- 国产 ROM（MIUI/HyperOS、ColorOS、OriginOS、OneUI、HarmonyOS）在 S+ 之后支持的设备。

### 2.2 取舍点（首版不动，后续可考虑）

- **"是否给用户提供强制关闭动态取色"的开关**：首版不做。理由：动态取色是"惊喜体验"，用户首次见到由壁纸派生的色调会感到原生；MVP 不增加设置项。
- **"动态取色时是否叠加品牌主色 brand-soft"**：首版**不做**叠加（保持 M3 派生色板的纯净性）；若用户反馈"识别不出装修管家"，再加品牌色锁定。
- **"是否要 surface 强制米色"**：首版**不做**；让背景随 `dynamicColorScheme` 变化。Web 的米色氛围留给 7–11 的静态色板承接。

---

## 3. 静态色板建议（Android 7–11 回退方案）

来源：以 Web 版 `css/style.css:2-19` 现有色为种子，参照 Google 官方 [Material Theme Builder](https://m3.material.io/theme-builder) 用 `cf6b45` 生成的"Reply codelab 风格"色板体系手工估算（M3 派生色为 13 tone 的色阶，下表只列与 Web 一一对应的关键 tone；其它 tone 由 `lightColorScheme` 默认回退）。

> **重要标注**：下列 hex 全部是"待视觉确认"草案。最终色应由设计师用 Material Theme Builder 输入 `cf6b45` 重新生成后给出，且需要在深浅两套上分别验 WCAG AA（正文 4.5:1，大字 3:1）。

### 3.1 浅色 LightColors（Android 7–11 + 12+ 用户强制静态时使用）

| M3 角色 | 草案 hex | 对应 Web 角色 | 说明 |
|---|---|---|---|
| `primary` | `#9C4324` | `brand-dark` | brand darker tone，保证 onPrimary 白色对比度 ≥ 4.5:1 |
| `onPrimary` | `#FFFFFF` | 文字色 | |
| `primaryContainer` | `#FFDBCD` | `brand-soft` | 提示性背景 |
| `onPrimaryContainer` | `#361000` | 暗文字 | |
| `secondary` | `#77574C` | brand 暖棕中性化 | |
| `onSecondary` | `#FFFFFF` | | |
| `secondaryContainer` | `#FFDBCD` | `brand-soft` | |
| `onSecondaryContainer` | `#2C150D` | | |
| `tertiary` | `#6B5D2F` | 暖琥珀（呼应 `--amber`） | 增强强调 |
| `onTertiary` | `#FFFFFF` | | |
| `tertiaryContainer` | `#F5E1A7` | `--amber-soft` | |
| `onTertiaryContainer` | `#231B00` | | |
| `background` | `#FFF8F4` | 接近 `--bg #f6f3ee` 略提亮 | 整体页面底 |
| `onBackground` | `#1F1B16` | 接近 `--ink` | |
| `surface` | `#FFF8F4` | 与 `background` 同步 | 卡片底 |
| `onSurface` | `#1F1B16` | | |
| `surfaceVariant` | `#F5E0D3` | 暖灰底（介于 line 与 card 之间） | 分类块、Tab 容器 |
| `onSurfaceVariant` | `#53433D` | | |
| `outline` | `#85736C` | `--muted` 系 | 分割线、边框 |
| `outlineVariant` | `#D7C2B8` | `--line` | 弱分割线 |
| `error` | `#BA1A1A` | 保留 M3 默认 | |
| `onError` | `#FFFFFF` | | |
| `errorContainer` | `#FFDAD6` | `--red-soft` | |
| `onErrorContainer` | `#410002` | | |
| `scrim` | `#000000` | | |
| `surfaceTint` | `#9C4324` | 同 primary | M3 用作 tonal elevation 起点 |

### 3.2 深色 DarkColors（Android 7–11 走系统深色时使用）

| M3 角色 | 草案 hex | 说明 |
|---|---|---|
| `primary` | `#FFB59A` | brand 提亮 tone |
| `onPrimary` | `#5B1B00` | |
| `primaryContainer` | `#7B2E10` | |
| `onPrimaryContainer` | `#FFDBCD` | |
| `secondary` | `#E7BDA9` | |
| `onSecondary` | `#44291E` | |
| `tertiary` | `#D8C58E` | 暖琥珀 |
| `onTertiary` | `#3A2E05` | |
| `background` | `#1F1B16` | 与 Web `--ink` 同源 |
| `onBackground` | `#EAE1D9` | |
| `surface` | `#1F1B16` | |
| `onSurface` | `#EAE1D9` | |
| `surfaceVariant` | `#53433D` | |
| `onSurfaceVariant` | `#D7C2B8` | |
| `outline` | `#A08C84` | |
| `outlineVariant` | `#53433D` | |
| `error` | `#FFB4AB` | |
| `errorContainer` | `#93000A` | |
| `onError` | `#690005` | |
| `onErrorContainer` | `#FFDAD6` | |

> 上述 dark 系列是为了"暗色模式仍保持暖调"，不至于像纯灰阶那么冷；正式色仍需 Material Theme Builder 跑 brand seed `cf6b45` 之后校准。

### 3.3 与 Web 现有色的差异

| 角色 | Web 直读 | M3 Light 草案 | 差异 | 备注 |
|---|---|---|---|---|
| 主背景 | `--bg #f6f3ee` | `background #FFF8F4` | 提亮 6 tone | 卡片白底需要比页面底更亮才能浮出，所以 M3 习惯把 `background` 拉高 |
| 卡片白 | `--card #ffffff` | `surface #FFF8F4` | 反而没有纯白 | 妥协：M3 派生的 surface 与 background 同色，靠 tonalElevation 区分；如果坚持纯白卡片，可以**手动覆盖 `surface` 为 `#FFFFFF`** |
| 品牌主 | `--brand #cf6b45` | `primary #9C4324` | 压暗 6 tone | 为了让 `onPrimary` 用白字达到 4.5:1（实际未压暗时约 3.4:1） |
| 文字 | `--ink #33312c` | `onSurface #1F1B16` | 略压暗 | M3 推荐正文 4.5:1 时多用偏黑值 |
| 弱文字 | `--muted #8b8479` | `onSurfaceVariant #53433D` | 较深 | 这是为满足"正文 4.5:1"硬性 WCAG AA 的代价 |

**结论**：直接 1:1 把 Web 16 进制贴到 M3 上**是过不了 WCAG AA 的**。M3 派生色板会自动按 tone 比例平衡对比度，所以静态色板必须重新派生。

---

## 4. 与 Web 版视觉差异对照表

> 注：差异是不可避免的；目标是"在 12+ 上让用户感到原生惊喜，在 7-11 上让用户感到像同一个 App"。

| 维度 | Web 版 | Android 7-11（静态） | Android 12+（动态） |
|---|---|---|---|
| 主背景 | `#f6f3ee` 恒为米色 | `#FFF8F4` 暖白 | 随壁纸 HCT 派生的浅 surface |
| 卡片 | `#ffffff` 纯白浮于米色 | `surface` = `background`（靠 tonalElevation 区分）或手动覆盖为 `#FFFFFF` | 派生色板中 `surface` 与 `background` 同色，靠 elevation |
| 主色按钮 | `--brand #cf6b45` 橙色按钮 | `primary #9C4324` 较深陶土橙 | 派生（可能是蓝/绿/紫，与品牌色可能完全不同） |
| 顶栏 / status bar | `<meta theme-color="#f6f3ee">` | `WindowCompat.setDecorFitsSystemWindows + statusBarColor=primary` 或透明 + contentScrim | 派生色板中 `primary` |
| 圆角 | 16px | 16dp（M3 default `medium`） | 同 |
| 字体 | 系统 + PingFang SC 栈 | 同，Compose `FontFamily.Default` 自动接 PingFang | 同 |
| 暗色 | 不支持（仅系统 dark 模式切换浏览器） | 通过 `isSystemInDarkTheme()` 切到 DarkColors | `dynamicDarkColorScheme(context)` 自动切 |
| 品牌识别度 | 强（橙 + 米色记忆点） | 强（米色系 + 橙色） | 弱（如果用户壁纸偏冷，App 会变冷色调） |
| 风险 | — | 无 | 见 §6 风险点 |

---

## 5. Compose `MaterialTheme` 接入伪代码

> 提示：以下为伪代码，仅供结构参考；不要直接当作可编译 Kotlin 提交。

```kotlin
// 文件位置建议：app/src/main/java/.../ui/theme/Theme.kt
// 依赖：androidx.compose.material3:material3

@Composable
fun RenovationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 允许在测试 / 设置页 / 主题切换里强制关闭
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors      // §3.2 静态
        else -> LightColors          // §3.1 静态
    }

    // 状态栏 / 导航栏颜色随 primary 走（Edge-to-Edge 之后改为 transparent + contentScrim）
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = RenovationTypography,   // Type.kt 中另行定义
        shapes      = RenovationShapes,       // Shape.kt 中另行定义，RoundedCornerShape(16.dp) 为主
        content     = content
    )
}
```

```kotlin
// 文件位置：app/src/main/java/.../ui/theme/ExtendedTheme.kt
// 目的：把"品牌主色"作为硬约束注入，避免被 dynamicColorScheme 冲掉

@Immutable
data class RenovationBrandColors(
    val brand: Color,           // 强提示色：tab 选中、FAB、关键文字
    val brandContainer: Color,  // 弱提示底
    val onBrandContainer: Color,
)

val LocalBrandColors = staticCompositionLocalOf<RenovationBrandColors> {
    error("RenovationBrandColors not provided")
}

@Composable
fun RenovationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = /* ... 上面 colorScheme 选择逻辑 ... */

    val brandColors = if (darkTheme) {
        RenovationBrandColors(
            brand            = Color(0xFFFFB59A),
            brandContainer   = Color(0xFF7B2E10),
            onBrandContainer = Color(0xFFFFDBCD),
        )
    } else {
        RenovationBrandColors(
            brand            = Color(0xFFCF6B45),   // 与 Web --brand 同色，不为通过 WCAG 压暗
            brandContainer   = Color(0xFFF9EBE4),   // 与 Web --brand-soft 同色
            onBrandContainer = Color(0xFF361000),
        )
    }

    CompositionLocalProvider(LocalBrandColors provides brandColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography  = RenovationTypography,
            shapes      = RenovationShapes,
            content     = content
        )
    }
}

// 业务层读取：
//  val brand = LocalBrandColors.current.brand
//  Button(colors = ButtonDefaults.buttonColors(containerColor = brand)) { ... }
```

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RenovationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()    // 5 Tab + Wizard
                }
            }
        }
    }
}
```

> 关于 `CompositionLocal` 注入的取舍：见 §6 风险点。`ExtendedTheme` 的成本是"在多色板并存时要双份维护"，收益是"关键品牌色不被 dynamicColor 冲掉"。**首版建议不引入 `ExtendedTheme`，直接用 `MaterialTheme.colorScheme.primary`**；等运营反馈"动态模式下 App 没有识别度"再加。

---

## 6. 风险点与回退策略

### 6.1 对比度 / WCAG AA

- **风险**：当 dynamicColorScheme 从壁纸派生出高饱和度（蓝/紫/绿）时，与 `onSurface`（`#1F1B16`）的对比度可能仍在 4.5:1 以上，但 `onPrimary` 上的辅助文字可能跌到 3:1 边缘。
- **回退**：
  - 不在 dynamicColor 路径上叠加品牌色硬约束（`ExtendedTheme`）；
  - 关键 CTA（保存、删除）使用 `MaterialTheme.colorScheme.error` / `tertiary`（M3 已保证 ≥ 4.5:1）；
  - 用 Compose `androidx.compose.ui.semantics` + TalkBack 测试覆盖。

### 6.2 暗色模式是否首版做

- **建议**：**首版做暗色**，因为 `isSystemInDarkTheme()` 是单行成本；不做反而显得"不原生"。
- 暗色实现：直接用 `isSystemInDarkTheme()` 选 `DarkColors` / `LightColors`，无需在 `MaterialTheme` 外做额外分支。
- **不做**"App 内手动切换深浅模式"（即首版不暴露"夜间模式"设置项），交由系统控制。

### 6.3 动态色板与 Web 品牌米色的冲突

- **风险**：用户壁纸是冷色（如蓝/灰）时，App 整体跑出冷色调，与"米色装修管家"的品牌记忆不一致。
- **回退**：
  1. 在"我的 / 设置"页加一个"始终使用品牌色板"开关（首版**不做**，登记为后续工单）；
  2. 用 `ExtendedTheme`（§5）锁定关键品牌色，不让 dynamicColor 完全接管（首版**不做**）；
  3. 把 `AppBar` / 顶栏 `background` 强制为 Web 米的近似值（`#FFF8F4`），让 AppBar 仍能"识别出是装修管家"，其余 body 走动态（首版**不做**，等反馈再定）。

### 6.4 系统状态栏 / 边到边

- **风险**：Android 12+ 走边到边（`WindowCompat.setDecorFitsSystemWindows(window, false)`）后，状态栏颜色需要用 `enableEdgeToEdge()` 或手动 `window.statusBarColor`；与 dynamicColor 的 `surface` 配合时容易出现"状态栏透明 + 顶栏米色但底色随壁纸"导致的割裂。
- **回退**：伪代码中已用 `SideEffect` 同步 `statusBarColor = colorScheme.primary.toArgb()`，但 Android 15+ `Window.setStatusBarColor` 已被废弃；建议改用 `enableEdgeToEdge()` + `SystemBarStyle.auto(...)`，由 system bar 跟随 surface。

### 6.5 饱和度过高 / 与原 Web 设计偏离

- **风险**：动态取色生成的 primary 经常是 Vivid HCT（高饱和度），与 Web 那个"柔和陶土橙"感觉不同。
- **回退**：这是 Material You 的本意，让 App 跟着系统调；只要不在 UI 中大量铺大色面（保持 Web 的"米色背景 + 小色块高亮"），用户感受不会"辣眼"。

### 6.6 Surface 与 Background 的同色问题

- **风险**：M3 默认 `surface == background`，靠 `tonalElevation` 区分；与 Web 的"白卡浮于米底"语义不同。
- **回退**：在静态色板分支手动将 `surface` 设为 `#FFFFFF`（覆盖 LightColors），让卡片维持 Web 视觉；在 dynamicColor 分支不动。

### 6.7 字体 / 排版未覆盖

- **不在本任务范围**。提一笔：Compose M3 默认 `Typography` 与 Web 系统字体栈（PingFang SC / Microsoft YaHei）实际由系统提供，不需要单独加载字体；后续若要加 "思源黑体"等再议。

---

## 7. 外部参考资料

- [Material Design 3 in Compose（官方）](https://developer.android.google.cn/develop/ui/compose/designsystems/material3) — 给出官方 dynamic color 写法（"Dynamic color schemes"小节）
- [androidx.compose.material3 package summary](https://developer.android.google.cn/reference/kotlin/androidx/compose/material3/package-summary) — `dynamicLightColorScheme` / `dynamicDarkColorScheme` 的 `RequiresApi(31)` 注解
- [Theming in Compose with Material 3（官方 codelab）](https://codelabs.developers.google.cn/codelabs/jetpack-compose-theming) — 完整 `AppTheme(darkTheme, content)` 签名
- [Custom design systems in Compose](https://developer.android.google.cn/develop/ui/compose/designsystems/custom) — `ExtendedTheme` / `CompositionLocalProvider` 模式
- [Material Theme Builder（官方）](https://m3.material.io/theme-builder) — 用 brand seed `cf6b45` 自动生成全部 13 tone 派生色，导出 Compose 代码
- [Material 3 Color Roles](https://m3.material.io/styles/color/the-color-system/key-colors-tones) — 5 key colors / 13 tones 体系
- [WCAG 2.1 Contrast Minimum](https://www.w3.org/TR/WCAG/#contrast-minimum) — 4.5:1 / 3:1 阈值
- [Accompanist 主页](https://google.github.io/accompanist/) — 之前的 `Dynamic Theme` 库已 Deprecated 并入 Material3 官方，本任务**不要**再引入 accompanist

---

## 8. Caveats / Not Found

- **未验证**：Android 12+ 国产 ROM（MIUI/HyperOS、ColorOS、OriginOS、HarmonyOS）对 `dynamicColorScheme` 的支持度。理论上这些 ROM 都已经升级到 Android 12+ 内核，应有系统壁纸取色；但部分深度定制 ROM 可能改写了 framework，表现为"换了壁纸但 App 颜色不变"。需要在国产机真机上验证，否则需要降级为"始终走静态色板"。
- **未验证**：Android 7-11 的市场占比（本任务假设 `minSdk = 24`）。若用户群体集中在 12+，可以反过来把 dynamicColor 设为默认开启、静态色板为降级。
- **未找到**：Material 3 在 Android 7-11 上的"system tone 近似"——即 `dynamicColorScheme` 在 S 以下没有任何 API 等价物，必须用静态色板。本结论来自 [Material 3 in Compose 官方文档](https://developer.android.google.cn/develop/ui/compose/designsystems/material3#dynamic-color-schemes) 直读。
- **未细化**：折叠屏 / 平板上的 status bar / navigation bar 分栏色彩策略，PRD 已声明"不做横屏大屏专门适配"，故本任务忽略。
- **静态色板 hex 全部为草案**：必须由设计师在 Material Theme Builder 用 seed `cf6b45` 跑一遍后给到正式 hex；本任务的"待视觉确认"标注见 §3 表格底部。
