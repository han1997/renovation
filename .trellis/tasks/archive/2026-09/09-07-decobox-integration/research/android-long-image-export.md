# Research: Android 清单导出长图方案对比

- **Query**: 把装修清单（纯文本 + 简单表格/分隔线）渲染成竖向长图，保存到相册或系统分享；对比 Compose 离屏渲染 / Canvas+StaticLayout 手绘 / WebView 截图三种方案
- **Scope**: mixed（内部代码事实 + 官方文档外部调研）
- **Date**: 2026-09-07

---

## 0. 项目现状事实（内部）

| 事实 | 出处 |
|---|---|
| minSdk 24 / targetSdk 35 / compileSdk 37，单 module `:app` | `android/gradle/libs.versions.toml:38-40` |
| Compose BOM 2026.08.00（解析为 Compose UI 1.12.0） | `android/gradle/libs.versions.toml:14`、`:35-36` 注释 |
| 依赖清单全是 androidx + kotlinx，**无 WebView / 无额外图形库** | `android/app/build.gradle.kts:80-131` |
| 现有导出模式：SAF `CreateDocument` + `rememberLauncherForActivityResult`，ViewModel 只出纯数据 | `android/app/src/main/java/com/renovation/guardian/ui/nav/AppRoot.kt:74-121` |
| 规范约定：「平台能力（SAF 文件选择 / 分享）在 Screen 层用 rememberLauncherForActivityResult，ViewModel 只提供纯数据」 | `.trellis/spec/android/ui-theme.md:25` |
| `AndroidManifest.xml` 目前**没有** FileProvider 声明，`res/xml/` 下无 file_paths | `android/app/src/main/AndroidManifest.xml` |
| 已有 CSV/JSON 导出纯函数先例（数据组装与 IO 分离） | `ui/budget/Csv.kt`、`data/repo/ImportExportRepository.kt` |
| 测试栈含 Robolectric 4.16.1（`isIncludeAndroidResources = true`） | `android/app/build.gradle.kts:73-77`、`libs.versions.toml:32` |

一个需要纠正的前置认知：任务描述中「API 34+ 的 `GraphicsLayer.record`」不准确。**Compose 的 `GraphicsLayer` API 与平台 API level 无关**，它随 Compose UI 1.7.0-alpha07 引入、1.7 起稳定，`toImageBitmap()` 官方说明为「API 22+ 硬件加速渲染（覆盖 99% 设备），API 21 回退软件渲染」。本项目 Compose 1.12.0 已包含该 API，minSdk 24 下**无需平台级回退**。平台级对应物 `HardwareRenderer`/`RenderNode` 才是 API 29+，`android.graphics.Picture` 是 API 1（注意：API 23 之前 Picture 不能在硬件加速画布上回放，minSdk 24 已越过）。

---

## 1. 方案对比

### 方案 A：Compose `@Composable` 离屏渲染 → `GraphicsLayer` → Bitmap

**实现要点**（官方文档给出的标准写法）：

```kotlin
val graphicsLayer = rememberGraphicsLayer()
Box(modifier = Modifier.drawWithContent {
    graphicsLayer.record { this@drawWithContent.drawContent() }
    drawLayer(graphicsLayer)
}) { /* 清单长图 Composable（固定宽度、非 Lazy） */ }

// 点击导出时：
coroutineScope.launch {
    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
    // 压缩保存 / 分享
}
```

- 内容**必须在组合中且完成绘制**才能 record：导出按钮触发时若长图未上屏，需要先把导出内容放进组合（常见做法：放进一个 0 尺寸或屏幕外的 Box，等首帧绘制完成后 record），时序靠 `Modifier.drawWithContent` 首帧回调驱动，属于隐式时序耦合。
- 内容必须用普通 `Column`（固定宽度、可无限高），**不能用 `LazyColumn`**（Lazy 只渲染可见视口，离屏内容不会被 record）。
- ⚠️ 官方 release note 明确（Compose 近期版本修复，`b/389046242`）：record 必须用 **DrawScope 里的 `GraphicsLayer#record` 扩展函数**，不要调 `GraphicsLayer` 的成员函数，否则有 NPE 风险。

**minSdk 兼容性**：Compose `GraphicsLayer`/`rememberGraphicsLayer`/`toImageBitmap` 在 API 22+ 硬件加速可用，minSdk 24 无需平台回退；不需要新增任何依赖（BOM 2026.08.00 → Compose UI 1.12.0 已含）。

**依赖**：零新增。

**风险**：

1. **长图高度受 GPU 纹理限制**：`toImageBitmap()` 走硬件加速路径，产出的是硬件位图，受 `GL_MAX_TEXTURE_SIZE`（常见 4096/8192/16384 px）约束。清单条目多时竖向高度轻松过万 px，超限后在部分设备上渲染失败或被裁切。
2. **硬件位图压缩隐患**：若 `toImageBitmap()` 返回 `Bitmap.Config.HARDWARE` 的位图，`Bitmap.compress()` 在部分版本上不可靠（硬件位图不允许 CPU 直接读像素）；稳妥做法是先 `copy(Config.ARGB_8888, false)` 到软件位图再压缩——又是一次全图内存拷贝。
3. **Robolectric 不可测**：新版 Compose 已「Disable software rendering support for the GraphicsLayer API」，渲染环节无法在 JVM 单测覆盖，只能手测。
4. UI 耦合：渲染逻辑长在 Composable 里，与「ViewModel 提供纯数据」的项目约定契合度一般。

### 方案 B：纯 `android.graphics.Canvas` + `StaticLayout` 手绘（无 Compose 依赖）

**实现要点**：

1. 纯 Kotlin 渲染器：输入 = 清单数据 + 样式（颜色/字号/密度），输出 = `Bitmap`，与 UI 无关。
2. 两遍布局：先构造各段 `StaticLayout.Builder.obtain(text, 0, len, textPaint, contentWidthPx)` 逐段测量高度（`layout.height`），累加得到总高度（标题、卡片、表格行、分隔线、页脚各占一段）；再 `Bitmap.createBitmap(width, totalHeight, ARGB_8888)` + `Canvas(bitmap)` 自上而下逐段 `layout.draw(canvas)` / `canvas.drawRect` 画分隔线与表格底色。
3. `StaticLayout.Builder` API 23+ 可用（minSdk 24 ✅），文本换行、中英混排、emoji 全部由系统排版引擎处理。
4. Material 3 风格靠「传参对齐」：在 Composable 层取 `MaterialTheme.colorScheme` / 字号 / `LocalDensity`，折算成 px 塞进样式对象传给渲染器（见 §4 骨架），视觉与 App 一致但颜色硬编码进绘制层之外。

**minSdk 兼容性**：API 24 全兼容，无任何版本分叉；全软件渲染，输出确定性 100%。

**依赖**：零新增（framework API）。

**风险**：

1. **内存/高度上限**：ARGB_8888 软件位图 = 宽 × 高 × 4 字节。1080 px 宽 × 20000 px 高 ≈ 86 MB，低内存设备可能 `OutOfMemoryError`；但软件位图**没有 GPU 纹理上限**，只受 RAM 约束，且可精确预算（先量总高再分配）。缓解：导出前若 `总像素 > maxPixels`（如 3000 万）自动降 density 重试，或 catch OOM 后半倍重绘（骨架已含）。
2. 表格/排版代码要自己写（约 200-300 行一次性代码），后续改样式要动绘制代码；清单是「纯文本+简单表格+分隔线」，复杂度可控。
3. 默认字体是平台字体（sans-serif / 思源黑体系），与 Compose 侧自定义字体若有差异需手动传 `Typeface`。

### 方案 C：WebView 加载 HTML 截图

**实现要点**：拼 HTML 字符串 → `WebView` 离屏加载 → `onPageFinished` 后测量内容总高 → `webView.measure + layout` → `canvas.drawBitmap` 截全高。

**评估结论：不推荐**，理由：

1. **依赖与体积**：本 App 是纯 Compose、全依赖清单里没有 WebView 相关使用，为一个导出功能引入 WebView 渲染栈（首启加载、Chromium 进程内存几十 MB 起步）违背依赖克制。
2. **时序脆弱**：HTML/字体/CSS 加载是异步的，`onPageFinished` 与实际排版完成存在竞态（图片/字体晚到会截半张）；离屏 WebView 的 measure 时机在各 ROM 上表现不一。
3. **视觉双轨维护**：长图样式要再用 HTML/CSS 写一遍，与 Compose 主题完全脱节，暗色模式、Material You 取色都要二次适配。
4. 全高截图 API 兼容性差：API 26 以下需要 `enableSlowWholeDocumentDraw()`，`capturePicture` 已废弃。
5. 相比方案 B，唯一优势是「会用 CSS 的人写样式快」，但代价是引入整套不确定性，对一个文本+表格场景完全不划算。

### 对比总表

| 维度 | A：Compose GraphicsLayer | B：Canvas + StaticLayout | C：WebView 截图 |
|---|---|---|---|
| minSdk 24 兼容 | ✅（Compose 1.7+，与平台版本无关） | ✅（StaticLayout.Builder API 23+） | ⚠️（需 `enableSlowWholeDocumentDraw` 等分叉） |
| 新增依赖 | 无 | 无 | 无（但引入 WebView 运行时开销） |
| 长图高度上限 | ⚠️ GPU 纹理上限（4096–16384px 常见）+ 硬件位图 | 仅 RAM（可预算、可重试），无 GPU 限制 | 仅 RAM（WebView measure 全高） |
| Robolectric 可单测 | ❌（软件渲染支持已禁用） | ✅（纯软件渲染） | ❌ |
| 与 M3 主题一致性 | 天然一致（直接复用 Composable） | 靠传参对齐（一次性适配） | 需 HTML/CSS 重写样式 |
| UI 时序耦合 | 高（必须进组合+完成绘制，非 Lazy） | 无（纯函数，任意线程调用） | 高（异步加载竞态） |
| 实现代码量 | 中 | 中偏多（手绘表格） | 中偏多（HTML 模板 + 截图时序） |

---

## 2. 保存与分享

### 2.1 保存到相册：MediaStore（API 29+，无需任何权限）

API 29+ 向 `MediaStore.Images` 插入**自己拥有的**条目不需要存储权限，配合 `IS_PENDING` 防止半成品外泄（官方文档 `developer.android.com/training/data-storage/shared/media`）：

```kotlin
suspend fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RenovationGuardian")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val uri = context.contentResolver.insert(collection, values) ?: return null
    try {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)   // PNG 无损；长图文件偏大可换 JPEG(90)
        }
    } catch (t: Throwable) {
        context.contentResolver.delete(uri, null, null); return null
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    }
    return uri
}
```

**API 24–28 的低版本写法（评估后不推荐采用）**：`Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)` + `WRITE_EXTERNAL_STORAGE` 运行时权限（声明需 `android:maxSdkVersion="28"`）。本项目目标用户主要在 10+ 机型，为老机型引入运行时权限链路（声明 + 请求 + 拒绝降级）不划算。

> **建议的兼容策略**：「保存到相册」按钮仅在 API 29+ 展示；API 24–28 只提供「分享长图」（见 2.2，零权限，用户可在系统分享面板里选相册类 App 保存）。targetSdk 35 下 `WRITE_EXTERNAL_STORAGE` 在新机型上本就无效，无需声明。

### 2.2 分享：FileProvider + ACTION_SEND（标准做法，全版本零权限）

当前 Manifest **未声明** FileProvider，需要新增三处：

**① `AndroidManifest.xml`**（`<application>` 内）：

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

（`androidx.core` 已是 activity-compose 的传递依赖，无需新增库。）

**② `app/src/main/res/xml/file_paths.xml`**（新建）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="exports" path="exports/" />
</paths>
```

**③ 分享代码**：

```kotlin
fun shareLongImage(context: Context, bitmap: Bitmap, fileName: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "$fileName.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)   // 必须 setFlags/addFlags，勿用 grantUriPermission
    }
    context.startActivity(Intent.createChooser(intent, "分享清单长图"))
}
```

官方文档强调：**禁止 `Uri.fromFile()`**（Gmail 等 target 无 READ_EXTERNAL_STORAGE 会直接失败），必须走 FileProvider 的 `content://` URI + `FLAG_GRANT_READ_URI_PERMISSION`；路径若不在 file_paths 声明范围内，`getUriForFile` 抛 `IllegalArgumentException`。API 29+ 也可以直接分享 MediaStore 返回的 `content://` URI，免去临时文件（两种并存，临时文件方案可同时服务 <29）。

---

## 3. 推荐结论

**推荐方案 B：纯 `Canvas` + `StaticLayout` 手绘渲染器**，理由（对齐本项目约束）：

1. **依赖克制（零新增）**、minSdk 24 全兼容、无版本分叉——渲染器是纯 Kotlin 纯函数 `数据 → Bitmap`，完美契合 `.trellis/spec/android/ui-theme.md:25`「平台能力在 Screen 层、ViewModel 只提供纯数据」的分层约定（渲染器放 `util/` 或 `ui/budget/`，与 `Csv.kt` 同级同风格）。
2. **Robolectric 可单测**：方案 A 的 GraphicsLayer 渲染环节在新版 Compose 已禁软件渲染，本项目又重仓 Robolectric 单测；方案 B 的长图高度计算（总高 = Σ段高）、数据截断规则可以直接 JVM 单测覆盖。
3. **高度上限更可控**：软件 ARGB_8888 位图无 GPU 纹理上限，只受 RAM 约束且可精确预算（先测总高再建位图）+ OOM 降级重试；方案 A 超长内容会撞 GL 纹理上限且产出硬件位图压缩还有坑。
4. **内容形态匹配**：清单 = 纯文本 + 简单表格 + 分隔线，StaticLayout 恰是官方为「Canvas 上多行文本排版」设计的 API，不存在复杂图形需求。
5. M3 视觉一致性通过「Composable 层取 MaterialTheme 色/字号/密度 → 传样式对象」一次性对齐，代价是约 200-300 行一次性绘制代码。

**备选**：若团队更看重「所见即所得」直接复用 Compose 布局，可选方案 A（项目 Compose 1.12.0 已含该 API，零依赖），但须接受 GPU 高度上限、硬件位图压缩拷贝、无法单测渲染、导出内容必须进组合绘制这四点。方案 C 否决。

---

## 4. 参考代码骨架（方案 B）

```kotlin
// util/ChecklistLongImage.kt —— 纯 Kotlin 渲染器，可 Robolectric 单测
package com.renovation.guardian.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/** 长图样式：由 Composable 层从 MaterialTheme 取值折算 px 后传入（颜色禁硬编码进本文件）。 */
data class LongImageStyle(
    val contentWidthPx: Int,     // 如 1080
    val bgColor: Int,
    val cardBgColor: Int,
    val dividerColor: Int,
    val titleColor: Int,
    val bodyColor: Int,
    val accentColor: Int,
    val titleSizePx: Float,      // dp * density
    val bodySizePx: Float,
)

data class LongImageRow(val label: String, val value: String, val note: String? = null)

object ChecklistLongImageRenderer {

    /** 全软件渲染；OOM 时降半倍重试一次。*/
    fun render(context: Context, title: String, subtitle: String, rows: List<LongImageRow>, style: LongImageStyle): Bitmap {
        return try {
            renderInternal(title, subtitle, rows, style)
        } catch (_: OutOfMemoryError) {
            val half = style.copy(
                contentWidthPx = style.contentWidthPx / 2,
                titleSizePx = style.titleSizePx / 2,
                bodySizePx = style.bodySizePx / 2,
            )
            renderInternal(title, subtitle, rows, half)
        }
    }

    private fun renderInternal(title: String, subtitle: String, rows: List<LongImageRow>, s: LongImageStyle): Bitmap {
        val margin = (s.contentWidthPx * 0.06f).toInt()
        val contentW = s.contentWidthPx - margin * 2

        // —— 1. 预排版（测总高）——
        val titleLayout = staticLayout(title, titlePaint(s, Typeface.BOLD), contentW)
        val subtitleLayout = staticLayout(subtitle, bodyPaint(s), contentW)
        val rowLayouts = rows.map { r ->
            Triple(staticLayout(r.label, bodyPaint(s), contentW), staticLayout(r.value, bodyPaint(s), contentW), r)
        }
        val rowHeight = rowLayouts.maxOf { (l, lv, _) -> maxOf(l.height, lv.height) }
        val colGap = (s.bodySizePx * 0.8f).toInt()
        val colSplit = contentW * 2 / 5   // label 2 : value 3 两列表格

        val totalH = margin + titleLayout.height + subtitleLayout.height +
                s.bodySizePx.toInt() /*间距*/ + rows.size * (rowHeight + /*行分隔*/1) +
                s.bodySizePx.toInt() * 2 /*页脚*/ + margin

        // —— 2. 位图 + Canvas 绘制 ——
        val bitmap = Bitmap.createBitmap(s.contentWidthPx, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val divider = Paint().apply { color = s.dividerColor; strokeWidth = 1f }

        canvas.drawColor(s.bgColor)
        var y = margin
        titleLayout.draw(canvas, margin, y); y += titleLayout.height
        subtitleLayout.draw(canvas, margin, y); y += subtitleLayout.height + s.bodySizePx.toInt()

        val cardRect = Paint().apply { color = s.cardBgColor }
        canvas.drawRoundRect(0f, y.toFloat(), s.contentWidthPx.toFloat(),
            (y + rows.size * (rowHeight + 1)).toFloat(), 24f, 24f, cardRect)

        rowLayouts.forEachIndexed { i, (label, value, row) ->
            if (i > 0) canvas.drawLine(margin.toFloat(), y.toFloat(),
                (s.contentWidthPx - margin).toFloat(), y.toFloat(), divider)
            canvas.save(); canvas.translate(margin.toFloat(), y.toFloat()); label.draw(canvas); canvas.restore()
            canvas.save(); canvas.translate((margin + colSplit + colGap).toFloat(), y.toFloat()); value.draw(canvas); canvas.restore()
            y += rowHeight + 1
        }
        y += s.bodySizePx.toInt()   // 页脚留白
        // 页脚小字：staticLayout("由装修管家生成 · 2026-09-07", ...) → draw
        return bitmap
    }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(true)
            .build()

    private fun titlePaint(s: LongImageStyle, style: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = s.titleColor; textSize = s.titleSizePx; typeface = Typeface.create("sans-serif-medium", style)
    }
    private fun bodyPaint(s: LongImageStyle) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = s.bodyColor; textSize = s.bodySizePx
    }

    private fun StaticLayout.draw(canvas: Canvas, x: Int, y: Int) {
        canvas.save(); canvas.translate(x.toFloat(), y.toFloat()); draw(canvas); canvas.restore()
    }
}
```

```kotlin
// ui 层胶水：Screen 里取主题 + 调渲染 + 保存/分享（对齐 AppRoot.kt 的 rememberExportActions 模式）
@Composable
fun rememberLongImageExporter(onDone: (Boolean) -> Unit): (Bitmap, String) -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    return { bitmap, fileName ->
        scope.launch(Dispatchers.IO) {
            val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // MediaStore 保存（§2.1），失败再走分享兜底
                saveToGallery(ctx, bitmap, fileName) != null
            } else {
                shareLongImage(ctx, bitmap, fileName); true   // <29 只分享，零权限
            }
            withContext(Dispatchers.Main) { onDone(ok) }      // → showSnackbar「已保存」/「分享完成」
        }
    }
}

// 样式来源示例（Screen/ViewModel 组装处）：
// val style = LongImageStyle(
//     contentWidthPx = 1080,
//     bgColor = colorScheme.surfaceContainerLowest.toArgb(),
//     cardBgColor = colorScheme.surfaceContainer.toArgb(),
//     dividerColor = colorScheme.surfaceVariant.toArgb(),
//     titleColor = colorScheme.onSurface.toArgb(),
//     bodyColor = colorScheme.onSurfaceVariant.toArgb(),
//     accentColor = colorScheme.primary.toArgb(),
//     titleSizePx = with(density) { 22.sp.toPx() },
//     bodySizePx = with(density) { 14.sp.toPx() },
// )
```

---

## External References

- [Write contents of a composable to a bitmap — Jetpack Compose Graphics Modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers) — 方案 A 官方标准写法（`rememberGraphicsLayer` + `drawWithContent` + `record` + `toImageBitmap`），注明需 Compose 1.7.0-alpha07+
- [androidx.compose.ui release notes](https://developer.android.com/jetpack/androidx/releases/compose-ui) — `GraphicsLayer#toImageBitmap` 为 API 22+ 硬件加速渲染（1.7.0-alpha05 起）；近期版本修复 record 扩展函数 NPE（b/389046242）并禁用 GraphicsLayer 软件渲染支持
- [android.graphics.Picture](https://developer.android.com/reference/android/graphics/Picture) — API 1；API 23 起才能在硬件加速画布回放（minSdk 24 已越过，可作为离屏记录备选）
- [Access media files from shared storage](https://developer.android.com/training/data-storage/shared/media) — MediaStore 插入图片、`IS_PENDING` 流程、无需权限的归属判定
- [Sharing files — FileProvider](https://developer.android.com/training/secure-file-sharing/share-file) — FileProvider authority、file_paths、`FLAG_GRANT_READ_URI_PERMISSION`、禁用 `Uri.fromFile()` 的原因

## Related Specs

- `.trellis/spec/android/index.md` — 技术栈与 minSdk 事实源、编码注意事项
- `.trellis/spec/android/ui-theme.md:25` — 平台能力（文件/分享）在 Screen 层、ViewModel 只出纯数据的分层约定
- `.trellis/spec/android/directory-structure.md` — util/ 无状态纯函数、ui 分层边界（渲染器落位 `util/` 或 `ui/budget/`）

## Caveats / Not Found

- `GraphicsLayer#toImageBitmap()` 对超长内容（>16384px）的精确失败行为官方文档未明示，仅能从「硬件加速渲染」推断存在 GPU 纹理上限；方案 A 若被采用需真机实测。
- 本仓库无任何现存 FileProvider / MediaStore 使用，上述 Manifest 与 file_paths 均为需新增项。
- 长图高度上限的安全阈值（建议 `maxPixels` ≈ 3000 万像素，即 1080×28000px）是工程经验值，非官方数字；实现时应以「先测总高 → 超限降 density 重试」兜底。
