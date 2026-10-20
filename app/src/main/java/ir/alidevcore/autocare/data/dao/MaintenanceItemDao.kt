package ir.alidevcore.autocare.data.dao

import androidx.room.*
import ir.alidevcore.autocare.data.model.MaintenanceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceItemDao {
    @Query("SELECT * FROM maintenance_items WHERE vehicleId = :vehicleId ORDER BY category, name")
    fun getAllItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>>

    @Query("SELECT * FROM maintenance_items WHERE vehicleId = :vehicleId AND isActive = 1 ORDER BY category, name")
    fun getActiveItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>>

    @Query("SELECT * FROM maintenance_items WHERE id = :id")
    fun getItemById(id: Long): Flow<MaintenanceItem?>

    @Query("SELECT * FROM maintenance_items WHERE id = :id")
    suspend fun getItemByIdOnce(id: Long): MaintenanceItem?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: MaintenanceItem): Long

    @Update
    suspend fun updateItem(item: MaintenanceItem)

    @Delete
    suspend fun deleteItem(item: MaintenanceItem)

    @Query("SELECT * FROM maintenance_items WHERE vehicleId = :vehicleId AND category = :category ORDER BY name")
    fun getItemsByCategory(vehicleId: Long, category: String): Flow<List<MaintenanceItem>>

    @Query("SELECT * FROM maintenance_items WHERE vehicleId = :vehicleId AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')")
    fun searchItems(vehicleId: Long, query: String): Flow<List<MaintenanceItem>>

    @Query("SELECT DISTINCT category FROM maintenance_items WHERE vehicleId = :vehicleId ORDER BY category")
    fun getCategoriesForVehicle(vehicleId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM maintenance_items WHERE vehicleId = :vehicleId AND isActive = 1")
    suspend fun getActiveItemCount(vehicleId: Long): Int
}
