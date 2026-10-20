package ir.alidevcore.autocare

import ir.alidevcore.autocare.util.NumberUtil
import org.junit.Assert.*
import org.junit.Test

class NumberUtilTest {

    @Test
    fun testParsePersianNumber() {
        assertEquals(125000L, NumberUtil.parsePersianNumber("۱۲۵۰۰۰"))
        assertEquals(125000L, NumberUtil.parsePersianNumber("125000"))
        assertEquals(125000L, NumberUtil.parsePersianNumber(" ۱۲۵,۰۰۰ "))
        assertEquals(125000L, NumberUtil.parsePersianNumber("۱۲۵٬۰۰۰"))
        assertEquals(0L, NumberUtil.parsePersianNumber("۰"))
        assertNull(NumberUtil.parsePersianNumber("abc"))
        assertNull(NumberUtil.parsePersianNumber(""))
    }

    @Test
    fun testFormatKm() {
        val formatted = NumberUtil.formatKm(100000)
        assertTrue(formatted.contains("کیلومتر"))
        assertTrue(formatted.contains("100") || formatted.contains("۱۰۰"))
    }
}
