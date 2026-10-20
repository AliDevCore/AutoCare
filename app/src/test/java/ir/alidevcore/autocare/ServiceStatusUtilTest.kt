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

    @Test
    fun testFindRepresentativePlanOverdueByKm() {
        val now = 1000000L
        val currentKm = 100000L

        val planFuture = PlannedService(id = 1, itemId = 10, vehicleId = 1, targetKm = 105000L)
        val planOverdue = PlannedService(id = 2, itemId = 10, vehicleId = 1, targetKm = 99000L) // 1000 km overdue

        val selected = ServiceStatusUtil.findRepresentativePlan(
            services = listOf(planFuture, planOverdue),
            currentKm = currentKm,
            now = now
        )
        assertEquals(2L, selected?.id)
    }

    @Test
    fun testFindRepresentativePlanOverdueByDate() {
        val now = 2000000L
        val currentKm = 50000L

        val planUpcoming = PlannedService(id = 1, itemId = 10, vehicleId = 1, targetDate = now + 100000L)
        val planOverdue = PlannedService(id = 2, itemId = 10, vehicleId = 1, targetDate = now - 50000L) // past date

        val selected = ServiceStatusUtil.findRepresentativePlan(
            services = listOf(planUpcoming, planOverdue),
            currentKm = currentKm,
            now = now
        )
        assertEquals(2L, selected?.id)
    }

    @Test
    fun testFindRepresentativePlanUpcomingDateVsKmNormalized() {
        val now = 1000000L
        val currentKm = 100000L
        val kmThreshold = 500L
        val daysThreshold = 7
        val dayMs = 24 * 60 * 60 * 1000L

        // Plan A: 1 day remaining out of 7 days (proximity ratio = 1/7 = 0.14)
        val planA = PlannedService(id = 1, itemId = 10, vehicleId = 1, targetDate = now + dayMs)
        // Plan B: 450 km remaining out of 500 km (proximity ratio = 450/500 = 0.90)
        val planB = PlannedService(id = 2, itemId = 10, vehicleId = 1, targetKm = currentKm + 450L)

        val selected = ServiceStatusUtil.findRepresentativePlan(
            services = listOf(planB, planA),
            currentKm = currentKm,
            now = now,
            kmThreshold = kmThreshold,
            daysThreshold = daysThreshold
        )
        // Plan A has much higher relative urgency towards its deadline!
        assertEquals(1L, selected?.id)
    }

    @Test
    fun testFindRepresentativePlanIdenticalDeadlinesDeterministicTieBreaker() {
        val now = 1000000L
        val currentKm = 100000L

        val plan1 = PlannedService(id = 5, itemId = 10, vehicleId = 1, targetKm = 102000L)
        val plan2 = PlannedService(id = 2, itemId = 10, vehicleId = 1, targetKm = 102000L)

        val selected = ServiceStatusUtil.findRepresentativePlan(
            services = listOf(plan1, plan2),
            currentKm = currentKm,
            now = now
        )
        // Lower ID wins tie-breaker
        assertEquals(2L, selected?.id)
    }

    @Test
    fun testFindRepresentativePlanOnlyKmOrOnlyDate() {
        val now = 1000000L
        val currentKm = 100000L

        val planKmOnly = PlannedService(id = 1, itemId = 10, vehicleId = 1, targetKm = 105000L, targetDate = null)
        val planDateOnly = PlannedService(id = 2, itemId = 10, vehicleId = 1, targetKm = null, targetDate = now + 500000L)

        val selected = ServiceStatusUtil.findRepresentativePlan(
            services = listOf(planKmOnly, planDateOnly),
            currentKm = currentKm,
            now = now
        )
        assertNotNull(selected)
    }
}
