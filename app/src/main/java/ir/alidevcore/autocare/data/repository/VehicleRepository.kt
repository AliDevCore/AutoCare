package ir.alidevcore.autocare.data.repository

import ir.alidevcore.autocare.data.dao.VehicleDao
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.data.model.relations.VehicleWithStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao
) {
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()

    fun getVehicleById(id: Long): Flow<Vehicle?> = vehicleDao.getVehicleById(id)

    suspend fun getVehicleByIdOnce(id: Long): Vehicle? = vehicleDao.getVehicleByIdOnce(id)

    suspend fun insertVehicle(vehicle: Vehicle): Long = vehicleDao.insertVehicle(vehicle)

    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)

    suspend fun updateCurrentKm(vehicleId: Long, km: Long) = vehicleDao.updateCurrentKm(vehicleId, km)

    suspend fun getVehicleCount(): Int = vehicleDao.getVehicleCount()
}
