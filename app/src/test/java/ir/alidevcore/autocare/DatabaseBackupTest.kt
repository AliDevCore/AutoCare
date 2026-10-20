package ir.alidevcore.autocare

import ir.alidevcore.autocare.data.model.PlanStatus
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DatabaseBackupTest {

    @Test
    fun testJsonBackupStructureSerialization() {
        val root = JSONObject().apply {
            put("appName", "AutoCare")
            put("schemaVersion", 2)
            put("exportTimestamp", 1700000000000L)

            val vArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1L)
                    put("name", "پژو 206")
                    put("currentKm", 125000L)
                    put("year", 1395)
                    put("createdAt", 1700000000000L)
                })
            }
            put("vehicles", vArray)

            val iArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 10L)
                    put("vehicleId", 1L)
                    put("name", "روغن ترمز")
                    put("category", "سیستم ترمز")
                    put("intervalKm", 40000L)
                    put("intervalMonths", 24)
                    put("description", "سرویس دوره‌ای روغن ترمز")
                    put("isActive", true)
                    put("createdAt", 1700000000000L)
                })
            }
            put("items", iArray)

            val pArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 100L)
                    put("itemId", 10L)
                    put("vehicleId", 1L)
                    put("targetKm", 150000L)
                    put("targetDate", JSONObject.NULL)
                    put("notes", "")
                    put("status", PlanStatus.PENDING.name)
                    put("convertedToRecordId", JSONObject.NULL)
                    put("createdAt", 1700000000000L)
                })
            }
            put("plans", pArray)

            put("records", JSONArray())
            put("reminders", JSONArray())
        }

        val jsonString = root.toString()
        val parsed = JSONObject(jsonString)

        assertEquals("AutoCare", parsed.getString("appName"))
        assertEquals(2, parsed.getInt("schemaVersion"))
        assertEquals(1, parsed.getJSONArray("vehicles").length())
        assertEquals("پژو 206", parsed.getJSONArray("vehicles").getJSONObject(0).getString("name"))

        val planObj = parsed.getJSONArray("plans").getJSONObject(0)
        assertEquals(150000L, planObj.getLong("targetKm"))
        assertEquals("PENDING", planObj.getString("status"))
        assertTrue(planObj.isNull("targetDate"))
    }

    @Test
    fun testPlannedServiceNotesCleanWithoutCalculationSentence() {
        val rawNote = ""
        assertFalse(rawNote.contains("محاسبه خودکار"))

        val legacyNote = "محاسبه خودکار از ثبت فله‌ای قبلی (روغن ترمز)"
        assertTrue(legacyNote.contains("محاسبه خودکار"))
    }
}
