package ir.alidevcore.autocare.data.dao

import androidx.room.*
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceRecordDao {
    @Query("SELECT * FROM maintenance_records WHERE itemId = :itemId ORDER BY date DESC")
    fun getRecordsForItem(itemId: Long): Flow<List<MaintenanceRecord>>

    @Query("""
        SELECT r.* FROM maintenance_records r
        INNER JOIN maintenance_items i ON r.itemId = i.id
        WHERE i.vehicleId = :vehicleId
        ORDER BY r.date DESC
    """)
    fun getRecordsForVehicle(vehicleId: Long): Flow<List<MaintenanceRecord>>

    @Query("""
        SELECT r.* FROM maintenance_records r
        INNER JOIN maintenance_items i ON r.itemId = i.id
        WHERE i.vehicleId = :vehicleId
        ORDER BY r.date DESC
        LIMIT :limit
    """)
    fun getRecentRecordsForVehicle(vehicleId: Long, limit: Int = 10): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    suspend fun getRecordById(id: Long): MaintenanceRecord?

    @Query("SELECT * FROM maintenance_records WHERE itemId = :itemId ORDER BY date DESC LIMIT 1")
    suspend fun getLastRecordForItem(itemId: Long): MaintenanceRecord?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: MaintenanceRecord): Long

    @Update
    suspend fun updateRecord(record: MaintenanceRecord)

    @Delete
    suspend fun deleteRecord(record: MaintenanceRecord)

    @Query("""
        SELECT r.* FROM maintenance_records r
        INNER JOIN maintenance_items i ON r.itemId = i.id
        WHERE i.vehicleId = :vehicleId
        AND r.date >= :fromDate AND r.date <= :toDate
        ORDER BY r.date DESC
    """)
    fun getRecordsForVehicleInDateRange(vehicleId: Long, fromDate: Long, toDate: Long): Flow<List<MaintenanceRecord>>

    @Query("""
        SELECT COALESCE(SUM(cost), 0) FROM maintenance_records r
        INNER JOIN maintenance_items i ON r.itemId = i.id
        WHERE i.vehicleId = :vehicleId AND r.cost IS NOT NULL
        AND r.date >= :fromDate AND r.date <= :toDate
    """)
    suspend fun getTotalCostForVehicleInRange(vehicleId: Long, fromDate: Long, toDate: Long): Long

    @Query("""
        SELECT r.* FROM maintenance_records r
        INNER JOIN maintenance_items i ON r.itemId = i.id
        WHERE i.vehicleId = :vehicleId
        AND (:query = '' OR i.name LIKE '%' || :query || '%' OR r.description LIKE '%' || :query || '%' OR i.category LIKE '%' || :query || '%')
        AND (:category = '' OR i.category = :category)
        AND (:fromKm = -1 OR r.km >= :fromKm)
        AND (:toKm = -1 OR r.km <= :toKm)
        AND (:fromDate = -1 OR r.date >= :fromDate)
        AND (:toDate = -1 OR r.date <= :toDate)
        AND (:fromCost = -1 OR (r.cost IS NOT NULL AND r.cost >= :fromCost))
        AND (:toCost = -1 OR (r.cost IS NOT NULL AND r.cost <= :toCost))
        ORDER BY r.date DESC
    """)
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
    ): Flow<List<MaintenanceRecord>>

    // For cost analytics - group by month
    @Query("""
        SELECT strftime('%Y-%m', datetime(r.date / 1000, 'unixepoch')) as month,
               SUM(r.cost) as totalCost,
               COUNT(*) as recordCount
        FROM maintenance_records r
        INNER JOIN maintenance_items i ON r.itemId = i.id
        WHERE i.vehicleId = :vehicleId AND r.cost IS NOT NULL
        AND r.date >= :fromDate AND r.date <= :toDate
        GROUP BY month
        ORDER BY month
    """)
    suspend fun getMonthlyCostSummary(vehicleId: Long, fromDate: Long, toDate: Long): List<MonthlyCost>

    @Query("""
        SELECT i.category,
               SUM(r.cost) as totalCost,
               COUNT(*) as recordCount
        FROM maintenance_records r
        INNER JOIN maintenance_items i ON r.itemId = i.id
        WHERE i.vehicleId = :vehicleId AND r.cost IS NOT NULL
        AND r.date >= :fromDate AND r.date <= :toDate
        GROUP BY i.category
        ORDER BY totalCost DESC
    """)
    suspend fun getCostByCategory(vehicleId: Long, fromDate: Long, toDate: Long): List<CategoryCost>
}

data class MonthlyCost(val month: String, val totalCost: Long, val recordCount: Int)
data class CategoryCost(val category: String, val totalCost: Long, val recordCount: Int)
