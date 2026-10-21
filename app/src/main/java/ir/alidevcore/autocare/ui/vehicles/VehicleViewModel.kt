package ir.alidevcore.autocare.ui.vehicles

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.backup.BackupStats
import ir.alidevcore.autocare.data.backup.DatabaseBackupManager
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.model.relations.ServiceStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VehicleUiState(
    val vehicles: List<VehicleCardData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

data class VehicleCardData(
    val vehicle: Vehicle,
    val overdueCount: Int = 0,
    val upcomingCount: Int = 0
)

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val backupManager: DatabaseBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleUiState(isLoading = true))
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            vehicleRepository.getAllVehicles()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { vehicles ->
                    val cardData = vehicles.map { vehicle ->
                        val statuses = maintenanceRepository.computeItemStatuses(vehicle.id, vehicle.currentKm)
                        val overdueCount = statuses.count { it.status == ServiceStatus.OVERDUE }
                        val upcomingCount = statuses.count { it.status == ServiceStatus.UPCOMING }
                        VehicleCardData(vehicle, overdueCount, upcomingCount)
                    }
                    _uiState.update { it.copy(vehicles = cardData, isLoading = false) }
                }
        }
    }

    fun addVehicle(name: String, currentKm: Long, year: Int?) {
        viewModelScope.launch {
            try {
                vehicleRepository.insertVehicle(
                    Vehicle(name = name, currentKm = currentKm, year = year)
                )
                _uiState.update { it.copy(successMessage = "خودرو با موفقیت اضافه شد") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در ثبت خودرو: ${e.message}") }
            }
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                vehicleRepository.updateVehicle(vehicle)
                _uiState.update { it.copy(successMessage = "خودرو با موفقیت ویرایش شد") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در ویرایش خودرو: ${e.message}") }
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                vehicleRepository.deleteVehicle(vehicle)
                _uiState.update { it.copy(successMessage = "خودرو با موفقیت حذف شد") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در حذف خودرو: ${e.message}") }
            }
        }
    }

    fun exportDatabase(onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = backupManager.exportDatabase()
                backupManager.shareBackupFile(file)
                onSuccess(file)
            } catch (e: Exception) {
                onError("خطا در پشتیبان‌گیری: ${e.message}")
            }
        }
    }

    fun importDatabase(uri: Uri, onSuccess: (BackupStats) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.importDatabaseFromUri(uri)
            result.fold(
                onSuccess = { stats -> onSuccess(stats) },
                onFailure = { error -> onError(error.message ?: "خطا در بازیابی دیتابیس") }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
