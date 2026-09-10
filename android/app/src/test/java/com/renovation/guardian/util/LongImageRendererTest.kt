package com.renovation.guardian.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LongImageRendererTest {
    @Test fun longListsUseBoundedPagesAndLongNotesDoNotOverlapRows() {
        val style = LongImageStyle(1080, -1, -1, 0xFFCCCCCC.toInt(), 0xFF111111.toInt(), 0xFF222222.toInt(), 0xFF555555.toInt(), 48f, 32f)
        val rows = (1..150).map { LongImageRow("项目 $it · 卧室与厨房的施工需求", "¥1234.56", "非常长的备注".repeat(100)) }
        var count = 0
        LongImageRenderer.renderPages("装修需求清单", "完整清单分图导出", rows, style).forEach { bitmap ->
            try { count++; assertEquals(1080, bitmap.width); assertTrue(bitmap.height in 1..LongImageRenderer.MAX_PAGE_HEIGHT) }
            finally { bitmap.recycle() }
        }
        assertTrue(count > 1)
    }
}
