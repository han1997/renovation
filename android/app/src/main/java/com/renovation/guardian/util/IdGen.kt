package com.renovation.guardian.util

/** 小工具：把可空或非字母数字的字符串规整成 SQL / id 用 key。 */
object IdGen {
    private val seq = java.util.concurrent.atomic.AtomicLong(0)
    fun new(prefix: String): String {
        val now = System.currentTimeMillis().toString(36)
        val s = seq.incrementAndGet()
        val r = (0..3).map { ('a'..'z').random() }.joinToString("")
        return "${prefix}_${now}_${s}_${r}"
    }
}