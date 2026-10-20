package ir.alidevcore.autocare

import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.relations.ServiceStatus
import ir.alidevcore.autocare.util.JalaliDateUtil
import ir.alidevcore.autocare.util.ServiceStatusUtil
import org.junit.Assert.*
import org.junit.Test

class ServiceStatusUtilTest {

    @Test
    fun testComputeNextKmFromLastRecord() {
        val item = MaintenanceItem(
            id = 1,
            vehicleId = 1,
            name = "روغن موتور",
            category = "روغن‌ها",
            intervalKm = 5000L
        )
        val lastRecord = MaintenanceRecord(
            id = 1,
            itemId = 1,
            date = System.currentTimeMillis(),
            km = 100000L
        )

        val nextKm = ServiceStatusUtil.computeNextKm(lastRecord, item, null)
        assertEquals(105000L, nextKm)
    }

    @Test
    fun testComputeNextKmFromPlannedServiceOverridesCalculation() {
        val item = MaintenanceItem(
            id = 1,
            vehicleId = 1,
            name = "روغن موتور",
            category = "روغن‌ها",
            intervalKm = 5000L
        )
        val lastRecord = MaintenanceRecord(
            id = 1,
            itemId = 1,
            date = System.currentTimeMillis(),
            km = 100000L
        )
        val planned = PlannedService(
            id = 1,
            itemId = 1,
            vehicleId = 1,
            targetKm = 107000L // Custom planned km
        )

        val nextKm = ServiceStatusUtil.computeNextKm(lastRecord, item, planned)
        assertEquals(107000L, nextKm)
    }

    @Test
    fun testComputeNextDateFromLastRecord() {
        val item = MaintenanceItem(
            id = 1,
            vehicleId = 1,
            name = "فیلتر هوا",
            category = "فیلترها",
            intervalMonths = 6
        )
        val recordDate = JalaliDateUtil.jalaliToTimestamp(1403, 1, 15)
        val lastRecord = MaintenanceRecord(
            id = 1,
            itemId = 1,
            date = recordDate,
            km = 80000L
        )

        val nextDate = ServiceStatusUtil.computeNextDate(lastRecord, item, null)
        assertNotNull(nextDate)
        assertEquals("1403/07/15", JalaliDateUtil.timestampToJalaliString(nextDate!!))
    }

    @Test
    fun testStatusOverdueKm() {
        val currentKm = 106000L
        val nextKm = 105000L // 1000 km past due

        val status = ServiceStatusUtil.computeStatus(
            currentKm = currentKm,
            nextKm = nextKm,
            nextDate = null,
            kmThreshold = 500L
        )
        assertEquals(ServiceStatus.OVERDUE, status)
    }

    @Test
    fun testStatusUpcomingKm() {
        val currentKm = 104700L
        val nextKm = 105000L // 300 km left, within threshold of 500L

        val status = ServiceStatusUtil.computeStatus(
            currentKm = currentKm,
            nextKm = nextKm,
            nextDate = null,
            kmThreshold = 500L
        )
        assertEquals(ServiceStatus.UPCOMING, status)
    }

    @Test
    fun testStatusOkKm() {
        val currentKm = 102000L
        val nextKm = 105000L // 3000 km left, well above threshold

        val status = ServiceStatusUtil.computeStatus(
            currentKm = currentKm,
            nextKm = nextKm,
            nextDate = null,
            kmThreshold = 500L
        )
        assertEquals(ServiceStatus.OK, status)
    }

    @Test
    fun testStatusNoSchedule() {
        val status = ServiceStatusUtil.computeStatus(
            currentKm = 100000L,
            nextKm = null,
            nextDate = null
        )
        assertEquals(ServiceStatus.NO_SCHEDULE, status)
    }

    @Test
    fun testBothCriteriaEitherTriggersOverdue() {
        val now = System.currentTimeMillis()
        // Km is OK (1000 km away), but date is OVERDUE (yesterday)
        val yesterday = now - 24 * 60 * 60 * 1000L
        val status = ServiceStatusUtil.computeStatus(
            currentKm = 100000L,
            nextKm = 105000L,
            nextDate = yesterday
        )
        assertEquals(ServiceStatus.OVERDUE, status)
    }
}
