package ir.alidevcore.autocare.util

import java.util.Calendar
import java.util.Date

object JalaliDateUtil {

    /**
     * Converts a timestamp (epoch ms) to a Jalali date string formatted as "۱۴۰۳/۰۸/۱۵"
     */
    fun timestampToJalaliString(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val jalali = JalaliCalendar(cal)
        return "${jalali.year}/${jalali.month.toString().padStart(2, '0')}/${jalali.day.toString().padStart(2, '0')}"
    }

    /**
     * Converts a timestamp to Jalali date string with month name: "۱۵ آبان ۱۴۰۳"
     */
    fun timestampToJalaliStringWithMonthName(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val jalali = JalaliCalendar(cal)
        val monthName = getJalaliMonthName(jalali.month)
        return "${toPersianDigits(jalali.day)} $monthName ${toPersianDigits(jalali.year)}"
    }

    /**
     * Returns current timestamp
     */
    fun now(): Long = System.currentTimeMillis()

    /**
     * Returns a timestamp for a given Jalali date (year, month, day)
     */
    fun jalaliToTimestamp(year: Int, month: Int, day: Int): Long {
        val jalali = JalaliCalendar(year, month, day)
        val gregorian = jalali.toGregorian()
        return gregorian.timeInMillis
    }

    /**
     * Adds months to a timestamp using Jalali calendar
     */
    fun addMonthsToTimestamp(timestamp: Long, months: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val jalali = JalaliCalendar(cal)
        var newMonth = jalali.month + months
        var newYear = jalali.year
        while (newMonth > 12) {
            newMonth -= 12
            newYear++
        }
        while (newMonth < 1) {
            newMonth += 12
            newYear--
        }
        // Clamp day to month length
        val maxDay = getJalaliMonthLength(newYear, newMonth)
        val newDay = minOf(jalali.day, maxDay)
        return jalaliToTimestamp(newYear, newMonth, newDay)
    }

    /**
     * Gets Jalali month length
     */
    fun getJalaliMonthLength(year: Int, month: Int): Int {
        return when {
            month <= 6 -> 31
            month <= 11 -> 30
            else -> if (JalaliCalendar.isLeap(year)) 30 else 29
        }
    }

    /**
     * Returns number of days between two timestamps
     */
    fun daysBetween(from: Long, to: Long): Long {
        return (to - from) / (1000L * 60 * 60 * 24)
    }

    /**
     * Returns current Jalali year, month, day as Triple
     */
    fun getCurrentJalaliDate(): Triple<Int, Int, Int> {
        val jalali = JalaliCalendar()
        return Triple(jalali.year, jalali.month, jalali.day)
    }

    /**
     * Converts a Gregorian timestamp to a JalaliCalendar object
     */
    fun toJalali(timestamp: Long): JalaliCalendar {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return JalaliCalendar(cal)
    }

    fun getJalaliMonthName(month: Int): String = when (month) {
        1 -> "فروردین"
        2 -> "اردیبهشت"
        3 -> "خرداد"
        4 -> "تیر"
        5 -> "مرداد"
        6 -> "شهریور"
        7 -> "مهر"
        8 -> "آبان"
        9 -> "آذر"
        10 -> "دی"
        11 -> "بهمن"
        12 -> "اسفند"
        else -> ""
    }

    fun toPersianDigits(number: Int): String {
        return number.toString()
            .replace('0', '۰').replace('1', '۱').replace('2', '۲')
            .replace('3', '۳').replace('4', '۴').replace('5', '۵')
            .replace('6', '۶').replace('7', '۷').replace('8', '۸')
            .replace('9', '۹')
    }

    fun toPersianDigits(number: Long): String = toPersianDigits(number.toString())

    fun toPersianDigits(str: String): String {
        return str
            .replace('0', '۰').replace('1', '۱').replace('2', '۲')
            .replace('3', '۳').replace('4', '۴').replace('5', '۵')
            .replace('6', '۶').replace('7', '۷').replace('8', '۸')
            .replace('9', '۹')
    }

    /**
     * Formats a Jalali date for display. Returns the month and year only.
     * e.g., "آبان ۱۴۰۳"
     */
    fun getJalaliMonthYear(timestamp: Long): String {
        val jalali = toJalali(timestamp)
        return "${getJalaliMonthName(jalali.month)} ${toPersianDigits(jalali.year)}"
    }

    /**
     * Get start of a Jalali month as timestamp
     */
    fun getStartOfJalaliMonth(year: Int, month: Int): Long {
        return jalaliToTimestamp(year, month, 1)
    }

    /**
     * Get end of a Jalali month as timestamp
     */
    fun getEndOfJalaliMonth(year: Int, month: Int): Long {
        val lastDay = getJalaliMonthLength(year, month)
        val cal = Calendar.getInstance()
        cal.timeInMillis = jalaliToTimestamp(year, month, lastDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Get start of current Jalali year as timestamp
     */
    fun getStartOfCurrentJalaliYear(): Long {
        val (year, _, _) = getCurrentJalaliDate()
        return jalaliToTimestamp(year, 1, 1)
    }

    /**
     * Format timestamp as short date for lists
     */
    fun formatShortDate(timestamp: Long): String {
        val jalali = toJalali(timestamp)
        return "${toPersianDigits(jalali.year)}/${toPersianDigits(jalali.month).padStart(2, '۰')}/${toPersianDigits(jalali.day).padStart(2, '۰')}"
    }
}
