package ir.alidevcore.autocare.util

import java.util.Calendar

/**
 * Pure Kotlin implementation of the Solar Hijri (Jalali / Shamsi) calendar.
 * Completely self-contained with zero external dependencies.
 */
class JalaliCalendar {
    var year: Int = 0
        private set
    var month: Int = 0
        private set
    var day: Int = 0
        private set

    constructor() {
        fromGregorian(Calendar.getInstance())
    }

    constructor(calendar: Calendar) {
        fromGregorian(calendar)
    }

    constructor(year: Int, month: Int, day: Int) {
        this.year = year
        this.month = month
        this.day = day
    }

    private fun fromGregorian(calendar: Calendar) {
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        this.year = jy
        this.month = jm
        this.day = jd
    }

    fun toGregorian(): Calendar {
        val (gy, gm, gd) = jalaliToGregorian(year, month, day)
        val cal = Calendar.getInstance()
        cal.set(gy, gm - 1, gd, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    companion object {
        fun isLeap(year: Int): Boolean {
            val r = (year - if (year > 0) 474 else 473) % 2820 + 474
            val b = (r + 38) * 682 % 2816
            return b < 682
        }

        fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
            val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            var gy_val = gy - 1600
            val gm_val = gm - 1
            val gd_val = gd - 1

            var g_day_no = 365 * gy_val + (gy_val + 3) / 4 - (gy_val + 99) / 100 + (gy_val + 399) / 400
            for (i in 0 until gm_val) {
                g_day_no += if (i == 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) 29 else g_d_m[i + 1] - g_d_m[i]
            }
            g_day_no += gd_val

            var j_day_no = g_day_no - 79
            val j_np = j_day_no / 12053
            j_day_no %= 12053

            var jy_val = 979 + 33 * j_np + 4 * (j_day_no / 1461)
            j_day_no %= 1461

            if (j_day_no >= 366) {
                jy_val += (j_day_no - 1) / 365
                j_day_no = (j_day_no - 1) % 365
            }

            var jm_val = 0
            while (jm_val < 11 && j_day_no >= (if (jm_val < 6) 31 else 30)) {
                j_day_no -= if (jm_val < 6) 31 else 30
                jm_val++
            }

            return Triple(jy_val, jm_val + 1, j_day_no + 1)
        }

        fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
            val jy_val = jy - 979
            val jm_val = jm - 1
            val jd_val = jd - 1

            var j_day_no = 365 * jy_val + (jy_val / 33) * 8 + ((jy_val % 33 + 3) / 4)
            for (i in 0 until jm_val) {
                j_day_no += if (i < 6) 31 else 30
            }
            j_day_no += jd_val

            var g_day_no = j_day_no + 79

            var gy_val = 1600 + 400 * (g_day_no / 146097)
            g_day_no %= 146097

            var leap = true
            if (g_day_no >= 36525) {
                g_day_no--
                gy_val += 100 * (g_day_no / 36524)
                g_day_no %= 36524

                if (g_day_no >= 365) {
                    g_day_no++
                } else {
                    leap = false
                }
            }

            gy_val += 4 * (g_day_no / 1461)
            g_day_no %= 1461

            if (g_day_no >= 366) {
                leap = false
                g_day_no--
                gy_val += g_day_no / 365
                g_day_no %= 365
            }

            val g_d_m = intArrayOf(
                0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
            )
            var gm_val = 0
            while (gm_val < 12 && g_day_no >= g_d_m[gm_val + 1]) {
                g_day_no -= g_d_m[gm_val + 1]
                gm_val++
            }

            return Triple(gy_val, gm_val + 1, g_day_no + 1)
        }
    }
}
