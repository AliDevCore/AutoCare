package ir.alidevcore.autocare.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.data.model.relations.ItemWithLastRecord
import ir.alidevcore.autocare.data.model.relations.ServiceStatus
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.util.JalaliDateUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val vehicle: Vehicle? = null,
    val overdueItems: List<ItemWithLastRecord> = emptyList(),
    val upcomingItems: List<ItemWithLastRecord> = emptyList(),
    val recentRecords: List<RecordDisplay> = emptyList(),
    val pendingPlans: List<PlannedService> = emptyList(),
    val monthlyTotal: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showKmUpdateSuccess: Boolean = false
)

data class RecordDisplay(
    val record: MaintenanceRecord,
    val itemName: String,
    val dateDisplay: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            vehicleRepository.getVehicleById(vehicleId)
                .filterNotNull()
                .collect { vehicle ->
                    // Compute item statuses
                    val itemStatuses = maintenanceRepository.computeItemStatuses(vehicle.id, vehicle.currentKm)
                    val overdueItems = itemStatuses.filter { it.status == ServiceStatus.OVERDUE }
                    val upcomingItems = itemStatuses.filter { it.status == ServiceStatus.UPCOMING }

                    // Get recent records with item names
                    val recentRecords = mutableListOf<RecordDisplay>()
                    maintenanceRepository.getRecentRecordsForVehicle(vehicle.id, 5)
                        .first()
                        .forEach { record ->
                            val item = maintenanceRepository.getItemByIdOnce(record.itemId)
                            recentRecords.add(
                                RecordDisplay(
                                    record = record,
                                    itemName = item?.name ?: "نامشخص",
                                    dateDisplay = record.date?.let { JalaliDateUtil.timestampToJalaliStringWithMonthName(it) } ?: "فقط کارکرد"
                                )
                            )
                        }

                    // Get pending plans
                    val pendingPlans = maintenanceRepository.getPendingServicesForVehicle(vehicle.id).first()

                    // Monthly cost
                    val now = System.currentTimeMillis()
                    val startOfMonth = JalaliDateUtil.run {
                        val (y, m, _) = getCurrentJalaliDate()
                        getStartOfJalaliMonth(y, m)
                    }
                    val monthlyTotal = maintenanceRepository.getTotalCostForVehicleInRange(vehicle.id, startOfMonth, now)

                    _uiState.update {
                        it.copy(
                            vehicle = vehicle,
                            overdueItems = overdueItems,
                            upcomingItems = upcomingItems,
                            recentRecords = recentRecords,
                            pendingPlans = pendingPlans,
                            monthlyTotal = monthlyTotal,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun updateCurrentKm(newKm: Long) {
        viewModelScope.launch {
            vehicleRepository.updateCurrentKm(vehicleId, newKm)
            _uiState.update { it.copy(showKmUpdateSuccess = true) }
        }
    }

    fun clearKmUpdateSuccess() {
        _uiState.update { it.copy(showKmUpdateSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
