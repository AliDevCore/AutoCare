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
        daysThreshold: Int = DEFAULT_DAYS_THRESHOLD,
        lastDateRecord: MaintenanceRecord? = null
    ): ItemWithLastRecord {
        val nextKm = computeNextKm(lastRecord, item, pendingPlan)
        val dateBasis = if (lastRecord?.date != null) lastRecord else (lastDateRecord ?: lastRecord)
        val nextDate = computeNextDate(dateBasis, item, pendingPlan)
        val status = computeStatus(currentKm, nextKm, nextDate, kmThreshold, daysThreshold)

        return ItemWithLastRecord(
            itemId = item.id,
            vehicleId = item.vehicleId,
            itemName = item.name,
            category = item.category,
            intervalKm = item.intervalKm,
            intervalMonths = item.intervalMonths,
            isActive = item.isActive,
            lastRecordDate = dateBasis?.date,
            lastRecordKm = lastRecord?.km,
            nextKm = nextKm,
            nextDate = nextDate,
            status = status
        )
    }

    /**
     * Finds the representative planned service for an item among multiple pending services.
     * Evaluates both km and date deadlines using normalized urgency metrics.
     *
     * Priority Hierarchy:
     * 1. OVERDUE (due date in past OR vehicle currentKm >= targetKm). Earliest deadline comes first.
     * 2. UPCOMING (within kmThreshold OR daysThreshold). Closer relative progress towards deadline comes first.
     * 3. FUTURE (neither overdue nor upcoming). Earliest scheduled date/km comes first.
     * 4. NO_CRITERIA (neither km nor date specified).
     * Tie-breaker: plan.id (deterministic).
     */
    fun findRepresentativePlan(
        services: List<PlannedService>,
        currentKm: Long,
        now: Long = System.currentTimeMillis(),
        kmThreshold: Long = DEFAULT_KM_THRESHOLD,
        daysThreshold: Int = DEFAULT_DAYS_THRESHOLD
    ): PlannedService? {
        if (services.isEmpty()) return null
        if (services.size == 1) return services.first()

        val daysThresholdMs = daysThreshold.toLong() * 24 * 60 * 60 * 1000L

        return services.minWithOrNull(
            Comparator { p1, p2 ->
                val p1Status = getPlanUrgencyCategory(p1, currentKm, now, kmThreshold, daysThresholdMs)
                val p2Status = getPlanUrgencyCategory(p2, currentKm, now, kmThreshold, daysThresholdMs)

                if (p1Status != p2Status) {
                    return@Comparator p1Status.compareTo(p2Status)
                }

                // Both are in the same category; resolve by specific criteria:
                when (p1Status) {
                    0 -> { // OVERDUE: earliest due date/lowest km came first
                        val date1 = p1.targetDate ?: Long.MAX_VALUE
                        val date2 = p2.targetDate ?: Long.MAX_VALUE
                        if (date1 != date2) return@Comparator date1.compareTo(date2)

                        val km1 = p1.targetKm ?: Long.MAX_VALUE
                        val km2 = p2.targetKm ?: Long.MAX_VALUE
                        if (km1 != km2) return@Comparator km1.compareTo(km2)
                    }
                    1 -> { // UPCOMING: normalized proximity to deadline (0.0 = right now, 1.0 = at threshold)
                        val prox1 = getUpcomingProximity(p1, currentKm, now, kmThreshold, daysThresholdMs)
                        val prox2 = getUpcomingProximity(p2, currentKm, now, kmThreshold, daysThresholdMs)
                        val cmp = prox1.compareTo(prox2)
                        if (cmp != 0) return@Comparator cmp
                    }
                    2 -> { // FUTURE: earliest scheduled date, then earliest target km
                        val date1 = p1.targetDate ?: Long.MAX_VALUE
                        val date2 = p2.targetDate ?: Long.MAX_VALUE
                        if (date1 != date2) return@Comparator date1.compareTo(date2)

                        val km1 = p1.targetKm ?: Long.MAX_VALUE
                        val km2 = p2.targetKm ?: Long.MAX_VALUE
                        if (km1 != km2) return@Comparator km1.compareTo(km2)
                    }
                }

                // Deterministic secondary tie-breaker
                p1.id.compareTo(p2.id)
            }
        )
    }

    private fun getPlanUrgencyCategory(
        p: PlannedService,
        currentKm: Long,
        now: Long,
        kmThreshold: Long,
        daysThresholdMs: Long
    ): Int {
        val isKmOverdue = p.targetKm != null && currentKm >= p.targetKm
        val isDateOverdue = p.targetDate != null && now >= p.targetDate
        if (isKmOverdue || isDateOverdue) return 0 // OVERDUE

        val kmRemaining = p.targetKm?.let { it - currentKm }
        val dateRemaining = p.targetDate?.let { it - now }

        val isKmUpcoming = kmRemaining != null && kmRemaining in 0..kmThreshold
        val isDateUpcoming = dateRemaining != null && dateRemaining in 0..daysThresholdMs
        if (isKmUpcoming || isDateUpcoming) return 1 // UPCOMING

        if (p.targetKm != null || p.targetDate != null) return 2 // FUTURE

        return 3 // NO CRITERIA
    }

    private fun getUpcomingProximity(
        p: PlannedService,
        currentKm: Long,
        now: Long,
        kmThreshold: Long,
        daysThresholdMs: Long
    ): Double {
        var minRatio = 1.0
        p.targetKm?.let { tkm ->
            val rem = (tkm - currentKm).coerceAtLeast(0)
            if (rem <= kmThreshold) {
                val ratio = rem.toDouble() / kmThreshold.toDouble()
                if (ratio < minRatio) minRatio = ratio
            }
        }
        p.targetDate?.let { td ->
            val rem = (td - now).coerceAtLeast(0)
            if (rem <= daysThresholdMs) {
                val ratio = rem.toDouble() / daysThresholdMs.toDouble()
                if (ratio < minRatio) minRatio = ratio
            }
        }
        return minRatio
    }
}
