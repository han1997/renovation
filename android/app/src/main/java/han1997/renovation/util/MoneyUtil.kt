package han1997.renovation.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** 入库和累计一律使用分；输入不接受分以下精度，不把错误静默变成零。 */
object MoneyUtil {
    fun parseYuan(text: String): Long? = try {
        val input = text.trim()
        if (!input.matches(Regex("[+-]?[0-9]+(?:\\.[0-9]{1,2})?"))) null
        else BigDecimal(input).movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) { null } catch (_: NumberFormatException) { null }

    fun fromYuan(yuan: String): Long = requireNotNull(parseYuan(yuan)) { "金额格式不正确（最多两位小数）" }
    fun fromYuan(yuan: Double): Long {
        require(yuan.isFinite()) { "金额必须是有限数值" }
        return BigDecimal.valueOf(yuan).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }
    fun toYuan(cents: Long): Double = cents / 100.0
    fun input(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
    fun format(cents: Long): String = formatter("#,##0").format(BigDecimal.valueOf(cents, 2))
    fun formatFull(cents: Long): String = formatter("#,##0.##").format(BigDecimal.valueOf(cents, 2))
    private fun formatter(pattern: String) = DecimalFormat(pattern, DecimalFormatSymbols(Locale.ROOT)).apply {
        roundingMode = RoundingMode.DOWN
    }
}
