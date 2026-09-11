package han1997.renovation.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class MoneyUtilTest {
    @Test fun decimalInputIsExact() {
        assertEquals(1L, MoneyUtil.parseYuan("0.01"))
        assertEquals(29L, MoneyUtil.parseYuan("0.29"))
        assertEquals(123456L, MoneyUtil.parseYuan("1234.56"))
        assertEquals(Long.MAX_VALUE, MoneyUtil.parseYuan(MoneyUtil.input(Long.MAX_VALUE)))
    }
    @Test fun invalidInputIsNotZero() {
        listOf("", "NaN", "Infinity", "1.234", "1,23", "1e6", "92233720368547758.08").forEach { assertNull(it, MoneyUtil.parseYuan(it)) }
        assertThrows(IllegalArgumentException::class.java) { MoneyUtil.fromYuan("invalid") }
        assertThrows(IllegalArgumentException::class.java) { MoneyUtil.fromYuan(Double.NaN) }
        assertThrows(ArithmeticException::class.java) { MoneyUtil.fromYuan(Double.MAX_VALUE) }
    }
    @Test fun formattingDoesNotLoseCentsOrDependOnLocale() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,234.56", MoneyUtil.formatFull(123456L))
            assertEquals("0.29", MoneyUtil.formatFull(29L))
            assertEquals("-0.01", MoneyUtil.formatFull(-1L))
            assertEquals("1234.56", MoneyUtil.input(123456L))
        } finally { Locale.setDefault(old) }
    }
}
