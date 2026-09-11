package han1997.renovation.util

import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** 日期工具：把 `today` 收敛到单一函数，便于测试替换。 */
object DateUtil {
    fun today(): String = nowLocalDate().toString()

    fun nowLocalDate(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    /** `iso`(YYYY-MM-DD) 加 `days` 天（可为负）。 */
    fun plusDays(iso: String, days: Int): String =
        try {
            LocalDate.parse(iso).plus(days, DateTimeUnit.DAY).toString()
        } catch (_: Throwable) {
            iso
        }

    fun minusDays(iso: String, days: Int): String = plusDays(iso, -days)

    fun daysBetween(fromIso: String, toIso: String): Int {
        return try {
            val a = LocalDate.parse(fromIso)
            val b = LocalDate.parse(toIso)
            (b.toEpochDays() - a.toEpochDays()).toInt()
        } catch (_: Throwable) {
            0
        }
    }

    fun daysFromToday(iso: String, today: String = today()): Int? =
        try {
            daysBetween(today, iso)
        } catch (_: Throwable) {
            null
        }

    fun formatCN(iso: String): String {
        return try {
            val d = LocalDate.parse(iso)
            "${d.year}年${d.monthNumber}月${d.dayOfMonth}日"
        } catch (_: Throwable) {
            iso
        }
    }
}