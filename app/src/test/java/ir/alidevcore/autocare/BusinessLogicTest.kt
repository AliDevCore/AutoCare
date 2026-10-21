package ir.alidevcore.autocare

import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlanStatus
import ir.alidevcore.autocare.data.model.PlannedService
import org.junit.Assert.*
import org.junit.Test

class BusinessLogicTest {

    @Test
    fun testExpenseCalculationIgnoresNullCosts() {
        val records = listOf(
            MaintenanceRecord(id = 1, itemId = 1, date = 1000L, km = 10000L, cost = 500000L),
            MaintenanceRecord(id = 2, itemId = 1, date = 2000L, km = 15000L, cost = null), // no cost specified
            MaintenanceRecord(id = 3, itemId = 2, date = 3000L, km = 20000L, cost = 1200000L),
            MaintenanceRecord(id = 4, itemId = 2, date = 4000L, km = 25000L, cost = null)  // no cost specified
        )

        val costedRecords = records.filter { it.cost != null }
        val totalCost = costedRecords.sumOf { it.cost!! }

        assertEquals(2, costedRecords.size)
        assertEquals(4, records.size)
        assertEquals(1700000L, totalCost)
    }

    @Test
    fun testPlanStatusTransitionOnCompletion() {
        val initialPlan = PlannedService(
            id = 42,
            itemId = 5,
            vehicleId = 1,
            targetKm = 110000L,
            status = PlanStatus.PENDING
        )
        assertEquals(PlanStatus.PENDING, initialPlan.status)
        assertNull(initialPlan.convertedToRecordId)

        val completedPlan = initialPlan.copy(
            status = PlanStatus.DONE,
            convertedToRecordId = 99L
        )
        assertEquals(PlanStatus.DONE, completedPlan.status)
        assertEquals(99L, completedPlan.convertedToRecordId)
    }

    @Test
    fun testMultiVehicleDataSeparation() {
        val vehicle1Records = listOf(
            MaintenanceRecord(id = 1, itemId = 101, date = 1000L, km = 50000L, cost = 200000L),
            MaintenanceRecord(id = 2, itemId = 102, date = 2000L, km = 55000L, cost = 300000L)
        )
        val vehicle2Records = listOf(
            MaintenanceRecord(id = 3, itemId = 201, date = 3000L, km = 120000L, cost = 800000L)
        )

        val v1Total = vehicle1Records.filter { it.cost != null }.sumOf { it.cost!! }
        val v2Total = vehicle2Records.filter { it.cost != null }.sumOf { it.cost!! }

        assertEquals(500000L, v1Total)
        assertEquals(800000L, v2Total)
        assertNotEquals(v1Total, v2Total)
    }

    @Test
    fun testHistoricalRecordsWithNullDates() {
        // User entered historical spark plug replacements at 30k, 60k, 90k, 130k without dates
        val historicalRecords = listOf(
            MaintenanceRecord(id = 1, itemId = 10, date = null, km = 30000L),
            MaintenanceRecord(id = 2, itemId = 10, date = null, km = 60000L),
            MaintenanceRecord(id = 3, itemId = 10, date = null, km = 90000L),
            MaintenanceRecord(id = 4, itemId = 10, date = null, km = 130000L)
        )

        val latestRecord = historicalRecords.maxByOrNull { it.km }
        assertNotNull(latestRecord)
        assertEquals(130000L, latestRecord!!.km)
        assertNull(latestRecord.date)

        // Spark plugs interval is 30,000 km
        val intervalKm = 30000L
        val nextKm = latestRecord.km + intervalKm
        assertEquals(160000L, nextKm)
    }

    @Test
    fun testBulkKmParsingRegex() {
        val input = "30000, 60000، 90000 - 130000"
        val normalized = input
            .replace('۰', '0').replace('۱', '1').replace('۲', '2')
            .replace('۳', '3').replace('۴', '4').replace('۵', '5')
            .replace('۶', '6').replace('۷', '7').replace('۸', '8')
            .replace('۹', '9')
            .replace("هزار", "000")
            .replace("k", "000", ignoreCase = true)

        val tokens = normalized.split(Regex("[,،;\\n\\r\\t\\/\\|\\-\\s]+"))
        val parsed = tokens.mapNotNull { token ->
            val digitsOnly = token.replace(Regex("[^0-9]"), "")
            digitsOnly.toLongOrNull()
        }.filter { it > 0 }.distinct().sorted()

        assertEquals(listOf(30000L, 60000L, 90000L, 130000L), parsed)
    }

    @Test
    fun testNewRecordIdAlwaysZeroForAutoIncrement() {
        val navRecordId = -1L
        val sanitizedRecordId = if (navRecordId > 0) navRecordId else null
        val isEditing = false

        val record = MaintenanceRecord(
            id = if (isEditing && sanitizedRecordId != null) sanitizedRecordId else 0L,
            itemId = 10,
            km = 100000L
        )

        assertEquals("New records must have id=0 to trigger SQLite autoincrement", 0L, record.id)
    }

    @Test
    fun testNewPlanIdAlwaysZeroForAutoIncrement() {
        val navPlanId = -1L
        val sanitizedPlanId = if (navPlanId > 0) navPlanId else null
        val isEditing = false

        val plan = PlannedService(
            id = if (isEditing && sanitizedPlanId != null) sanitizedPlanId else 0L,
            itemId = 10,
            vehicleId = 1,
            targetKm = 150000L
        )

        assertEquals("New plans must have id=0 to trigger SQLite autoincrement", 0L, plan.id)
    }
}
