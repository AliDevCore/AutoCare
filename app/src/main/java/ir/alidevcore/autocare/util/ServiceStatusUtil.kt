package ir.alidevcore.autocare.util

import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.relations.ItemWithLastRecord
import ir.alidevcore.autocare.data.model.relations.ServiceStatus

object ServiceStatusUtil {

    const val DEFAULT_KM_THRESHOLD = 500L
    const val DEFAULT_DAYS_THRESHOLD = 7

    /**
     * Computes the next service km based on last record and item interval
     */
    fun computeNextKm(lastRecord: MaintenanceRecord?, item: MaintenanceItem, plannedService: PlannedService?): Long? {
        // If there's a pending planned service with target km, use that
        plannedService?.targetKm?.let { return it }
        // Otherwise compute from last record + interval
        if (lastRecord != null && item.intervalKm != null) {
            return lastRecord.km + item.intervalKm
        }
        return null
    }

    /**
     * Computes the next service date based on last record and item interval
     */
    fun computeNextDate(lastRecord: MaintenanceRecord?, item: MaintenanceItem, plannedService: PlannedService?): Long? {
        // If there's a pending planned service with target date, use that
        plannedService?.targetDate?.let { return it }
        // Otherwise compute from last record date + interval months
        if (lastRecord?.date != null && item.intervalMonths != null) {
            return JalaliDateUtil.addMonthsToTimestamp(lastRecord.date, item.intervalMonths)
        }
        return null
    }

    /**
     * Determines service status
     */
    fun computeStatus(
        currentKm: Long,
        nextKm: Long?,
        nextDate: Long?,
        kmThreshold: Long = DEFAULT_KM_THRESHOLD,
        daysThreshold: Int = DEFAULT_DAYS_THRESHOLD
    ): ServiceStatus {
        val now = System.currentTimeMillis()
        val daysThresholdMs = daysThreshold.toLong() * 24 * 60 * 60 * 1000L

        if (nextKm == null && nextDate == null) return ServiceStatus.NO_SCHEDULE

        var isOverdue = false
        var isUpcoming = false

        nextKm?.let { nkm ->
            when {
                currentKm >= nkm -> isOverdue = true
                (nkm - currentKm) <= kmThreshold -> isUpcoming = true
            }
        }

        nextDate?.let { nd ->
            when {
                now >= nd -> isOverdue = true
                (nd - now) <= daysThresholdMs -> isUpcoming = true
            }
        }

        return when {
            isOverdue -> ServiceStatus.OVERDUE
            isUpcoming -> ServiceStatus.UPCOMING
            else -> ServiceStatus.OK
        }
    }

    /**
     * Builds an ItemWithLastRecord from components
     */
    fun buildItemWithLastRecord(
        item: MaintenanceItem,
        lastRecord: MaintenanceRecord?,
        pendingPlan: PlannedService?,
        currentKm: Long,
        kmThreshold: Long = DEFAULT_KM_THRESHOLD,
        daysThreshold: Int = DEFAULT_DAYS_THRESHOLD
    ): ItemWithLastRecord {
        val nextKm = computeNextKm(lastRecord, item, pendingPlan)
        val nextDate = computeNextDate(lastRecord, item, pendingPlan)
        val status = computeStatus(currentKm, nextKm, nextDate, kmThreshold, daysThreshold)

        return ItemWithLastRecord(
            itemId = item.id,
            vehicleId = item.vehicleId,
            itemName = item.name,
            category = item.category,
            intervalKm = item.intervalKm,
            intervalMonths = item.intervalMonths,
            isActive = item.isActive,
            lastRecordDate = lastRecord?.date,
            lastRecordKm = lastRecord?.km,
            nextKm = nextKm,
            nextDate = nextDate,
            status = status
        )
    }
}
