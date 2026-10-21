package ir.alidevcore.autocare

import ir.alidevcore.autocare.util.JalaliDateUtil
import org.junit.Assert.*
import org.junit.Test

class JalaliDateUtilTest {

    @Test
    fun testPersianDigitsConversion() {
        assertEquals("۱۲۳۴۵۶۷۸۹۰", JalaliDateUtil.toPersianDigits(1234567890))
        assertEquals("۰", JalaliDateUtil.toPersianDigits(0))
        assertEquals("۱۴۰۳", JalaliDateUtil.toPersianDigits(1403))
    }

    @Test
    fun testJalaliMonthNames() {
        assertEquals("فروردین", JalaliDateUtil.getJalaliMonthName(1))
        assertEquals("مرداد", JalaliDateUtil.getJalaliMonthName(5))
        assertEquals("اسفند", JalaliDateUtil.getJalaliMonthName(12))
        assertEquals("", JalaliDateUtil.getJalaliMonthName(13))
    }

    @Test
    fun testJalaliMonthLengths() {
        assertEquals(31, JalaliDateUtil.getJalaliMonthLength(1403, 1))
        assertEquals(31, JalaliDateUtil.getJalaliMonthLength(1403, 6))
        assertEquals(30, JalaliDateUtil.getJalaliMonthLength(1403, 7))
        assertEquals(30, JalaliDateUtil.getJalaliMonthLength(1403, 11))
        // 1404 is a leap year in Jalali calendar (30 days in Esfand)
        assertEquals(30, JalaliDateUtil.getJalaliMonthLength(1404, 12))
        // 1399 was a leap year (30 days in Esfand)
        assertEquals(30, JalaliDateUtil.getJalaliMonthLength(1399, 12))
        // 1403 is not a leap year (29 days in Esfand)
        assertEquals(29, JalaliDateUtil.getJalaliMonthLength(1403, 12))
        // 1402 is not a leap year (29 days in Esfand)
        assertEquals(29, JalaliDateUtil.getJalaliMonthLength(1402, 12))
    }

    @Test
    fun testJalaliToTimestampAndBack() {
        val timestamp = JalaliDateUtil.jalaliToTimestamp(1403, 8, 15)
        val dateString = JalaliDateUtil.timestampToJalaliString(timestamp)
        assertEquals("1403/08/15", dateString)
    }

    @Test
    fun testAddMonthsToTimestamp() {
        // Start: 1403/02/10
        val startTs = JalaliDateUtil.jalaliToTimestamp(1403, 2, 10)
        // Add 6 months -> 1403/08/10
        val nextTs = JalaliDateUtil.addMonthsToTimestamp(startTs, 6)
        val nextDateStr = JalaliDateUtil.timestampToJalaliString(nextTs)
        assertEquals("1403/08/10", nextDateStr)

        // Cross year boundary: 1403/10/15 + 5 months -> 1404/03/15
        val yearBoundaryTs = JalaliDateUtil.jalaliToTimestamp(1403, 10, 15)
        val nextYearTs = JalaliDateUtil.addMonthsToTimestamp(yearBoundaryTs, 5)
        val nextYearDateStr = JalaliDateUtil.timestampToJalaliString(nextYearTs)
        assertEquals("1404/03/15", nextYearDateStr)
    }
}
