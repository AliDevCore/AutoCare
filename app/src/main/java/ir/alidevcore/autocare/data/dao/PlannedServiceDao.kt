package ir.alidevcore.autocare.data.dao

import androidx.room.*
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.PlanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedServiceDao {
    @Query("SELECT * FROM planned_services WHERE vehicleId = :vehicleId AND status = 'PENDING' ORDER BY targetDate ASC, targetKm ASC")
    fun getPendingServicesForVehicle(vehicleId: Long): Flow<List<PlannedService>>

    @Query("SELECT * FROM planned_services WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    fun getAllServicesForVehicle(vehicleId: Long): Flow<List<PlannedService>>

    @Query("SELECT * FROM planned_services WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun getServicesForItem(itemId: Long): Flow<List<PlannedService>>

    @Query("SELECT * FROM planned_services WHERE id = :id")
    suspend fun getServiceById(id: Long): PlannedService?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertService(service: PlannedService): Long

    @Update
    suspend fun updateService(service: PlannedService)

    @Delete
    suspend fun deleteService(service: PlannedService)

    @Query("UPDATE planned_services SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: PlanStatus)

    @Query("UPDATE planned_services SET status = 'DONE', convertedToRecordId = :recordId WHERE id = :planId")
    suspend fun markAsDone(planId: Long, recordId: Long)

    @Query("UPDATE planned_services SET status = 'CANCELLED' WHERE id = :planId")
    suspend fun markAsCancelled(planId: Long)

    @Query("SELECT * FROM planned_services WHERE vehicleId = :vehicleId AND status = 'PENDING' AND targetDate IS NOT NULL ORDER BY targetDate ASC")
    suspend fun getPendingServicesWithDateOnce(vehicleId: Long): List<PlannedService>

    @Query("SELECT * FROM planned_services WHERE status = 'PENDING' AND targetDate IS NOT NULL")
    suspend fun getAllPendingServicesWithDate(): List<PlannedService>
}
