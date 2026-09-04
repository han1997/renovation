package com.renovation.guardian.util

import kotlin.math.roundToLong

/** 金额格式化工具（cents ↔ 元）。 */
object MoneyUtil {
    private const val CENTS_PER_YUAN = 100L

    fun fromYuan(yuan: Double): Long = (yuan * CENTS_PER_YUAN).roundToLong()
    fun fromYuan(yuan: String): Long = fromYuan(yuan.toDoubleOrNull() ?: 0.0)
    fun toYuan(cents: Long): Double = cents.toDouble() / CENTS_PER_YUAN.toDouble()

    /** 形如 "12,345" 的整数显示。 */
    fun format(cents: Long): String {
        val yuan = toYuan(cents)
        val long = yuan.toLong()
        return "%,d".format(long)
    }

    fun formatFull(cents: Long): String {
        val yuan = toYuan(cents)
        val asLong = yuan.toLong()
        val frac = ((yuan - asLong) * 100).toInt().let { if (it < 0) -it else it }
        return if (frac == 0) "%,d".format(asLong) else "%,d.%02d".format(asLong, frac)
    }
}