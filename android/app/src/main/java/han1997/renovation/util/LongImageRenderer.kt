package han1997.renovation.util

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/** 颜色由 Compose 主题传入，位图不依赖 UI 树。 */
data class LongImageStyle(val contentWidthPx: Int, val bgColor: Int, val cardBgColor: Int,
    val dividerColor: Int, val titleColor: Int, val bodyColor: Int, val mutedColor: Int,
    val titleSizePx: Float, val bodySizePx: Float)
data class LongImageRow(val label: String, val value: String, val note: String? = null)

/** 按真实行高分页，每页最多 4096px，调用方逐张写入、释放，避免巨型位图。 */
object LongImageRenderer {
    const val MAX_PAGE_HEIGHT = 4096
    private data class RowLayout(val left: StaticLayout, val right: StaticLayout, val note: StaticLayout?, val height: Int)
    fun renderPages(title: String, subtitle: String, rows: List<LongImageRow>, style: LongImageStyle): Sequence<Bitmap> = sequence {
        val width = style.contentWidthPx.coerceIn(480, 1080)
        val margin = 40
        val usable = width - margin * 2
        fun paint(color: Int, size: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = size }
        val titlePaint = paint(style.titleColor, style.titleSizePx)
        val bodyPaint = paint(style.bodyColor, style.bodySizePx)
        val mutedPaint = paint(style.mutedColor, style.bodySizePx * .85f)
        fun layout(text: String, p: TextPaint, w: Int) = StaticLayout.Builder.obtain(text, 0, text.length, p, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(6f, 1f).build()
        val titleLayout = layout(title, titlePaint, usable)
        val subtitleLayout = layout(subtitle, mutedPaint, usable)
        val header = margin + titleLayout.height + subtitleLayout.height + 36
        var page = mutableListOf<RowLayout>()
        var height = header + margin + 48
        var number = 1
        fun draw(): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(style.bgColor)
            fun at(l: StaticLayout, x: Int, y: Int) { canvas.save(); canvas.translate(x.toFloat(), y.toFloat()); l.draw(canvas); canvas.restore() }
            at(titleLayout, margin, margin)
            at(subtitleLayout, margin, margin + titleLayout.height + 12)
            var y = header
            val divider = Paint().apply { color = style.dividerColor; strokeWidth = 1f }
            page.forEach { row ->
                at(row.left, margin, y + 12); at(row.right, margin + (usable * .57f).toInt(), y + 12)
                row.note?.let { at(it, margin, y + 20 + maxOf(row.left.height, row.right.height)) }
                y += row.height
                canvas.drawLine(margin.toFloat(), y.toFloat(), (width - margin).toFloat(), y.toFloat(), divider)
            }
            canvas.drawText("第 ${number++} 页 · 装修管家", margin.toFloat(), (height - 20).toFloat(), mutedPaint)
            return bitmap
        }
        rows.forEach { row ->
            // 超长备注分块保留全部内容；避免单个文本条目高过一页。
            val labels = row.label.chunked(200)
            val values = row.value.chunked(200)
            val notes = row.note.orEmpty().chunked(200)
            repeat(maxOf(labels.size, values.size, notes.size, 1)) { index ->
                val left = layout(labels.getOrElse(index) { "" }, bodyPaint, (usable * .54f).toInt())
                val right = layout(values.getOrElse(index) { "" }, bodyPaint, (usable * .43f).toInt())
                val note = notes.getOrNull(index)?.let { layout(it, mutedPaint, usable) }
                val rowHeight = maxOf(left.height, right.height) + (note?.height ?: 0) + 36
                if (page.isNotEmpty() && height + rowHeight > MAX_PAGE_HEIGHT) {
                    yield(draw()); page = mutableListOf(); height = header + margin + 48
                }
                require(height + rowHeight <= MAX_PAGE_HEIGHT) { "图片文字过大，请调整导出字号" }
                page.add(RowLayout(left, right, note, rowHeight)); height += rowHeight
            }
        }
        yield(draw())
    }
}
