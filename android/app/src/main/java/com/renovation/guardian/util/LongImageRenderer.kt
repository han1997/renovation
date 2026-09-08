package com.renovation.guardian.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * 长图样式:由 Compose 层从 MaterialTheme 取值折算 px 后传入(颜色禁硬编码进本文件)。
 */
data class LongImageStyle(
    val contentWidthPx: Int,
    val bgColor: Int,
    val cardBgColor: Int,
    val dividerColor: Int,
    val titleColor: Int,
    val bodyColor: Int,
    val mutedColor: Int,
    val titleSizePx: Float,
    val bodySizePx: Float,
)

/** 长图行(label 左,value 右;note 作为次级说明)。 */
data class LongImageRow(
    val label: String,
    val value: String,
    val note: String? = null,
)

/**
 * Canvas + StaticLayout 手绘长图渲染器(纯软件渲染,可 Robolectric 单测)。
 *
 * - 输入 = 标题 + 副标题 + 行数据 + 样式,输出 Bitmap,无 UI / 平台耦合;
 * - 两遍布局:先逐段测量总高,再建位图绘制;
 * - OOM 时降半倍分辨率重试一次。
 */
object LongImageRenderer {

    fun render(title: String, subtitle: String, rows: List<LongImageRow>, style: LongImageStyle): Bitmap =
        try {
            renderInternal(title, subtitle, rows, style)
        } catch (_: OutOfMemoryError) {
            val half = style.copy(
                contentWidthPx = style.contentWidthPx / 2,
                titleSizePx = style.titleSizePx / 2,
                bodySizePx = style.bodySizePx / 2,
            )
            renderInternal(title, subtitle, rows, half)
        }

    private fun renderInternal(
        title: String,
        subtitle: String,
        rows: List<LongImageRow>,
        s: LongImageStyle,
    ): Bitmap {
        val margin = (s.contentWidthPx * 0.05f).toInt().coerceAtLeast(24)
        val contentW = s.contentWidthPx - margin * 2

        val titlePaint = textPaint(s.titleColor, s.titleSizePx, Typeface.create("sans-serif-medium", Typeface.BOLD))
        val bodyPaint = textPaint(s.bodyColor, s.bodySizePx, Typeface.create("sans-serif", Typeface.NORMAL))
        val valuePaint = textPaint(s.bodyColor, s.bodySizePx, Typeface.create("sans-serif-medium", Typeface.NORMAL))
        val mutedPaint = textPaint(s.mutedColor, s.bodySizePx * 0.85f, Typeface.create("sans-serif", Typeface.NORMAL))

        val titleLayout = staticLayout(title, titlePaint, contentW)
        val subtitleLayout = staticLayout(subtitle, mutedPaint, contentW)
        val rowLayouts = rows.map { row ->
            Triple(
                staticLayout(row.label, bodyPaint, (contentW * 0.52f).toInt()),
                staticLayout(row.value, valuePaint, (contentW * 0.46f).toInt()),
                row.note?.let { staticLayout(it, mutedPaint, contentW) },
            )
        }
        val lineHeight = (s.bodySizePx * 1.5f).toInt()
        val rowHeight = lineHeight + (mutedPaint.fontMetrics.let { -it.ascent + it.descent } * 1.3f).toInt()

        // 预排版,累加总高
        val cardTop = margin + titleLayout.height + subtitleLayout.height + (s.bodySizePx * 0.8f).toInt()
        val cardRowsH = rows.size * rowHeight
        val footerH = (s.bodySizePx * 1.6f).toInt()
        val totalH = cardTop + cardRowsH + (s.bodySizePx).toInt() + footerH + margin

        val bitmap = Bitmap.createBitmap(s.contentWidthPx, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val dividerPaint = Paint().apply { color = s.dividerColor; strokeWidth = 1f }
        val cardPaint = Paint().apply { color = s.cardBgColor }
        canvas.drawColor(s.bgColor)

        var y = margin
        titleLayout.draw(canvas, margin, y)
        y += titleLayout.height
        subtitleLayout.draw(canvas, margin, y)
        y += subtitleLayout.height + (s.bodySizePx * 0.8f).toInt()

        // 卡片背景
        canvas.drawRoundRect(
            margin.toFloat(), y.toFloat(),
            (s.contentWidthPx - margin).toFloat(), (y + cardRowsH).toFloat(),
            20f, 20f, cardPaint,
        )

        rowLayouts.forEachIndexed { i, (label, value, note) ->
            val rowTop = y + i * rowHeight
            if (i > 0) {
                canvas.drawLine(margin.toFloat(), rowTop.toFloat(),
                    (s.contentWidthPx - margin).toFloat(), rowTop.toFloat(), dividerPaint)
            }
            label.draw(canvas, margin + 16, rowTop + 8)
            value.draw(canvas, s.contentWidthPx - margin - (contentW * 0.46f).toInt() - 16, rowTop + 8)
            if (note != null) {
                note.draw(canvas, margin + 16, rowTop + label.height + 4)
            }
        }
        return bitmap
    }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(true)
            .build()

    private fun textPaint(color: Int, size: Float, tf: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = tf
    }

    private fun StaticLayout.draw(canvas: Canvas, x: Int, y: Int) {
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        draw(canvas)
        canvas.restore()
    }
}
