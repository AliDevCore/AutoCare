package ir.alidevcore.autocare.data.repository

import ir.alidevcore.autocare.data.dao.MaintenanceItemDao
import ir.alidevcore.autocare.data.dao.MaintenanceRecordDao
import ir.alidevcore.autocare.data.dao.PlannedServiceDao
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.PlanStatus
import ir.alidevcore.autocare.data.model.relations.ItemWithLastRecord
import ir.alidevcore.autocare.data.model.relations.RecordWithItem
import ir.alidevcore.autocare.data.model.relations.ServiceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepository @Inject constructor(
    private val itemDao: MaintenanceItemDao,
    private val recordDao: MaintenanceRecordDao,
    private val plannedDao: PlannedServiceDao
) {

    // -------------------------------------------------------------------------
    // Items
    // -------------------------------------------------------------------------

    fun getAllItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>> =
        itemDao.getAllItemsForVehicle(vehicleId)

    fun getActiveItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>> =
        itemDao.getActiveItemsForVehicle(vehicleId)

    fun getItemById(id: Long): Flow<MaintenanceItem?> = itemDao.getItemById(id)

    suspend fun getItemByIdOnce(id: Long): MaintenanceItem? = itemDao.getItemByIdOnce(id)

    suspend fun insertItem(item: MaintenanceItem): Long = itemDao.insertItem(item)

    suspend fun updateItem(item: MaintenanceItem) = itemDao.updateItem(item)

    suspend fun deleteItem(item: MaintenanceItem) = itemDao.deleteItem(item)

    fun getCategoriesForVehicle(vehicleId: Long): Flow<List<String>> =
        itemDao.getCategoriesForVehicle(vehicleId)

    fun searchItems(vehicleId: Long, query: String): Flow<List<MaintenanceItem>> =
        itemDao.searchItems(vehicleId, query)

    // -------------------------------------------------------------------------
    // Records
    // -------------------------------------------------------------------------

    fun getRecordsForItem(itemId: Long): Flow<List<MaintenanceRecord>> =
        recordDao.getRecordsForItem(itemId)

    fun getRecordsForVehicle(vehicleId: Long): Flow<List<MaintenanceRecord>> =
        recordDao.getRecordsForVehicle(vehicleId)

    fun getRecentRecordsForVehicle(vehicleId: Long, limit: Int = 10): Flow<List<MaintenanceRecord>> =
        recordDao.getRecentRecordsForVehicle(vehicleId, limit)

    suspend fun getRecordById(id: Long): MaintenanceRecord? = recordDao.getRecordById(id)

    suspend fun getLastRecordForItem(itemId: Long): MaintenanceRecord? =
        recordDao.getLastRecordForItem(itemId)

    suspend fun insertRecord(record: MaintenanceRecord): Long = recordDao.insertRecord(record)

    suspend fun insertRecords(records: List<MaintenanceRecord>): List<Long> = recordDao.insertRecords(records)

    suspend fun updateRecord(record: MaintenanceRecord) = recordDao.updateRecord(record)

    suspend fun deleteRecord(record: MaintenanceRecord) = recordDao.deleteRecord(record)

    suspend fun getTotalCostForVehicleAllTime(vehicleId: Long): Long =
        recordDao.getTotalCostForVehicleAllTime(vehicleId)

    fun getRecordsForVehicleInDateRange(vehicleId: Long, fromDate: Long, toDate: Long): Flow<List<MaintenanceRecord>> =
        recordDao.getRecordsForVehicleInDateRange(vehicleId, fromDate, toDate)

    suspend fun getTotalCostForVehicleInRange(vehicleId: Long, fromDate: Long, toDate: Long): Long =
        recordDao.getTotalCostForVehicleInRange(vehicleId, fromDate, toDate)

    fun searchRecords(
        vehicleId: Long,
        query: String = "",
        category: String = "",
        fromKm: Long = -1,
        toKm: Long = -1,
        fromDate: Long = -1,
        toDate: Long = -1,
        fromCost: Long = -1,
        toCost: Long = -1
    ): Flow<List<MaintenanceRecord>> =
        recordDao.searchRecords(vehicleId, query, category, fromKm, toKm, fromDate, toDate, fromCost, toCost)

    suspend fun getMonthlyCostSummary(vehicleId: Long, fromDate: Long, toDate: Long) =
        recordDao.getMonthlyCostSummary(vehicleId, fromDate, toDate)

    suspend fun getCostByCategory(vehicleId: Long, fromDate: Long, toDate: Long) =
        recordDao.getCostByCategory(vehicleId, fromDate, toDate)

    // -------------------------------------------------------------------------
    // Planned Services
    // -------------------------------------------------------------------------

    fun getPendingServicesForVehicle(vehicleId: Long): Flow<List<PlannedService>> =
        plannedDao.getPendingServicesForVehicle(vehicleId)

    fun getAllServicesForVehicle(vehicleId: Long): Flow<List<PlannedService>> =
        plannedDao.getAllServicesForVehicle(vehicleId)

    fun getServicesForItem(itemId: Long): Flow<List<PlannedService>> =
        plannedDao.getServicesForItem(itemId)

    suspend fun getServiceById(id: Long): PlannedService? = plannedDao.getServiceById(id)

    suspend fun insertPlannedService(service: PlannedService): Long =
        plannedDao.insertPlannedService(service)

    suspend fun updatePlannedService(service: PlannedService) =
        plannedDao.updatePlannedService(service)

    suspend fun deletePlannedService(service: PlannedService) =
        plannedDao.deletePlannedService(service)

    suspend fun markPlanAsDone(planId: Long, recordId: Long) =
        plannedDao.markAsDone(planId, recordId)

    suspend fun markPlanAsCancelled(planId: Long) =
        plannedDao.markAsCancelled(planId)

    // -------------------------------------------------------------------------
    // Business Logic / Status Aggregation
    // -------------------------------------------------------------------------

    /**
     * Computes the current [ServiceStatus] and next service metrics for all active
     * items of a vehicle, combining the last recorded service with any pending
     * planned services.
     *
     * @param vehicleId   The ID of the vehicle whose item statuses should be computed.
     * @param currentKm   Latest odometer reading for the vehicle (in km).
     * @param kmThreshold Distance margin (km) that marks a service as UPCOMING (default 500 km).
     * @param daysThreshold Day margin that marks a service as UPCOMING (default 7 days).
     * @return A list of [ItemWithLastRecord] – one entry per active item, sorted by status severity
     *         (OVERDUE first, then UPCOMING, then OK, then NO_SCHEDULE).
     */
    suspend fun computeItemStatuses(
        vehicleId: Long,
        currentKm: Long,
        kmThreshold: Long = 500L,
        daysThreshold: Int = 7
    ): List<ItemWithLastRecord> {
        val items: List<MaintenanceItem> =
            itemDao.getActiveItemsForVehicle(vehicleId).first()

        val pendingServices: List<PlannedService> =
            plannedDao.getPendingServicesForVehicle(vehicleId).first()

        val now = System.currentTimeMillis()
        val serviceByItem: Map<Long, PlannedService> = pendingServices
            .groupBy { it.itemId }
            .mapValues { (_, services) ->
                services.minWithOrNull(
                    compareBy<PlannedService> { plan ->
                        // 1. Overdue plans get highest priority
                        val isKmOverdue = plan.targetKm != null && currentKm >= plan.targetKm
                        val isDateOverdue = plan.targetDate != null && now >= plan.targetDate
                        if (isKmOverdue || isDateOverdue) 0 else 1
                    }.thenBy { plan ->
                        // 2. Urgent target kilometers
                        plan.targetKm?.let { (it - currentKm).coerceAtLeast(0) } ?: Long.MAX_VALUE
                    }.thenBy { plan ->
                        // 3. Urgent target dates
                        plan.targetDate?.let { (it - now).coerceAtLeast(0) } ?: Long.MAX_VALUE
                    }
                )!!
            }

        return items
            .map { item ->
                val lastRecord: MaintenanceRecord? = recordDao.getLastRecordForItem(item.id)
                val plan: PlannedService? = serviceByItem[item.id]

                ir.alidevcore.autocare.util.ServiceStatusUtil.buildItemWithLastRecord(
                    item = item,
                    lastRecord = lastRecord,
                    pendingPlan = plan,
                    currentKm = currentKm,
                    kmThreshold = kmThreshold,
                    daysThreshold = daysThreshold
                )
            }
            .sortedWith(
                compareBy { statusSortOrder(it.status) }
            )
    }

    private fun statusSortOrder(status: ServiceStatus): Int = when (status) {
        ServiceStatus.OVERDUE -> 0
        ServiceStatus.UPCOMING -> 1
        ServiceStatus.OK -> 2
        ServiceStatus.NO_SCHEDULE -> 3
    }
}
