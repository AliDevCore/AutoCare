package ir.alidevcore.autocare

import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.util.ExcelExporter
import org.junit.Assert.*
import org.junit.Test

class ExcelAndCsvExportTest {

    @Test
    fun testCsvEscapingNormalText() {
        val result = ExcelExporter.escapeCsvCell("تعویض روغن موتور")
        assertEquals("\"تعویض روغن موتور\"", result)
    }

    @Test
    fun testCsvEscapingInternalDoubleQuotesRfc4180() {
        val result = ExcelExporter.escapeCsvCell("روغن 10W-40 برند \"کاسترول\" اصل")
        assertEquals("\"روغن 10W-40 برند \"\"کاسترول\"\" اصل\"", result)
    }

    @Test
    fun testCsvEscapingCommasAndNewlinesPreservedInQuotes() {
        val multiline = "خط اول\nخط دوم, با ویرگول"
        val result = ExcelExporter.escapeCsvCell(multiline)
        assertTrue(result.startsWith("\""))
        assertTrue(result.endsWith("\""))
        assertTrue(result.contains("خط اول\nخط دوم, با ویرگول"))
    }

    @Test
    fun testCsvFormulaInjectionEqualsTrigger() {
        val dangerous = "=cmd|' /C calc'!A0"
        val result = ExcelExporter.escapeCsvCell(dangerous)
        assertEquals("\"'=cmd|' /C calc'!A0\"", result)
    }

    @Test
    fun testCsvFormulaInjectionWithLeadingSpacesAndAtSign() {
        val dangerous = "   @SUM(1+1)*cmd|' /C calc'!A0"
        val result = ExcelExporter.escapeCsvCell(dangerous)
        assertEquals("\"'   @SUM(1+1)*cmd|' /C calc'!A0\"", result)
    }

    @Test
    fun testCsvFormulaInjectionWithLeadingTabAndMinus() {
        val dangerous = "\t-2+3+cmd|' /C notepad'!A0"
        val result = ExcelExporter.escapeCsvCell(dangerous)
        assertEquals("\"'\t-2+3+cmd|' /C notepad'!A0\"", result)
    }

    @Test
    fun testCsvFormulaInjectionWithPlusSign() {
        val dangerous = "+1+1"
        val result = ExcelExporter.escapeCsvCell(dangerous)
        assertEquals("\"'+1+1\"", result)
    }

    @Test
    fun testCsvNullValueSafe() {
        val result = ExcelExporter.escapeCsvCell(null)
        assertEquals("\"\"", result)
    }

    @Test
    fun testExcelSheetNameSanitizationForbiddenCharacters() {
        val existing = mutableSetOf<String>()
        val input = "خودرو [پژو 206] / فاز: 1? * test"
        val sanitized = ExcelExporter.sanitizeSheetName(input, existing)

        assertFalse(sanitized.contains("["))
        assertFalse(sanitized.contains("]"))
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("?"))
        assertFalse(sanitized.contains("*"))
        assertTrue(sanitized.length <= 31)
    }

    @Test
    fun testExcelSheetNameLengthLimit31() {
        val existing = mutableSetOf<String>()
        val veryLongName = "سوابق_تعمیرات_خودروی_پژو_پارس_مدل_1400_بسیار_طولانی"
        val sanitized = ExcelExporter.sanitizeSheetName(veryLongName, existing)

        assertTrue(sanitized.length <= 31)
    }

    @Test
    fun testExcelSheetNameDisambiguationDuplicates() {
        val existing = mutableSetOf<String>()
        val name1 = ExcelExporter.sanitizeSheetName("1_سوابق_پژو", existing)
        val name2 = ExcelExporter.sanitizeSheetName("1_سوابق_پژو", existing)
        val name3 = ExcelExporter.sanitizeSheetName("1_سوابق_پژو", existing)

        assertEquals("1_سوابق_پژو", name1)
        assertEquals("1_سوابق_پژو_1", name2)
        assertEquals("1_سوابق_پژو_2", name3)
        assertEquals(3, existing.size)
    }

    @Test
    fun testMaintenanceRecordsOrderingDateDescNullDateFallback() {
        val records = listOf(
            MaintenanceRecord(id = 1, itemId = 1, date = null, km = 30000L),
            MaintenanceRecord(id = 2, itemId = 1, date = 1700000000000L, km = 80000L),
            MaintenanceRecord(id = 3, itemId = 1, date = 1600000000000L, km = 50000L),
            MaintenanceRecord(id = 4, itemId = 1, date = null, km = 60000L)
        )

        // UI display sorting: newest date first, then highest km, then highest id
        val sortedForUi = records.sortedWith(
            compareByDescending<MaintenanceRecord> { it.date ?: 0L }
                .thenByDescending { it.km }
                .thenByDescending { it.id }
        )

        assertEquals(2L, sortedForUi[0].id) // 1700...
        assertEquals(3L, sortedForUi[1].id) // 1600...
        assertEquals(4L, sortedForUi[2].id) // null date, 60000 km
        assertEquals(1L, sortedForUi[3].id) // null date, 30000 km

        // Technical interval sorting by KM
        val sortedByKm = records.maxByOrNull { it.km }
        assertEquals(80000L, sortedByKm?.km)
    }
}
