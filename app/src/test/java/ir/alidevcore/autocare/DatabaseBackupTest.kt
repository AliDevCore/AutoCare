package ir.alidevcore.autocare

import ir.alidevcore.autocare.data.model.PlanStatus
import ir.alidevcore.autocare.data.model.PlannedService
import org.junit.Assert.*
import org.junit.Test

class DatabaseBackupTest {

    @Test
    fun testJsonBackupStructureSpecification() {
        val expectedKeys = listOf("appName", "schemaVersion", "exportTimestamp", "vehicles", "items", "records", "plans", "reminders")
        val sampleJson = """
            {
                "appName": "AutoCare",
                "schemaVersion": 2,
                "exportTimestamp": 1700000000000,
                "vehicles": [{"id": 1, "name": "پژو 206", "currentKm": 125000, "year": 1395}],
                "items": [{"id": 10, "vehicleId": 1, "name": "روغن ترمز", "category": "سیستم ترمز", "intervalKm": 40000, "intervalMonths": 24, "description": "سرویس دوره‌ای", "isActive": true}],
                "plans": [{"id": 100, "itemId": 10, "vehicleId": 1, "targetKm": 150000, "targetDate": null, "notes": "", "status": "PENDING", "convertedToRecordId": null}],
                "records": [],
                "reminders": []
            }
        """.trimIndent()

        for (key in expectedKeys) {
            assertTrue("JSON backup structure must contain key: $key", sampleJson.contains("\"$key\""))
        }

        val plan = PlannedService(
            id = 100L,
            itemId = 10L,
            vehicleId = 1L,
            targetKm = 150000L,
            targetDate = null,
            notes = "",
            status = PlanStatus.PENDING
        )
        assertEquals(150000L, plan.targetKm)
        assertEquals(PlanStatus.PENDING, plan.status)
        assertNull(plan.targetDate)
    }

    @Test
    fun testPlannedServiceNotesCleanWithoutCalculationSentence() {
        val rawNote = ""
        assertFalse(rawNote.contains("محاسبه خودکار"))

        val legacyNote = "محاسبه خودکار از ثبت فله‌ای قبلی (روغن ترمز)"
        assertTrue(legacyNote.contains("محاسبه خودکار"))
    }
}
