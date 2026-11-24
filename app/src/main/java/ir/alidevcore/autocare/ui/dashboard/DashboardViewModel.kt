package ir.alidevcore.autocare.ui.dashboard

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.backup.BackupStats
import ir.alidevcore.autocare.data.backup.DatabaseBackupManager
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.data.model.relations.ItemWithLastRecord
import ir.alidevcore.autocare.data.model.relations.ServiceStatus
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.repository.ReminderRepository
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.notification.ReminderScheduler
import ir.alidevcore.autocare.ui.items.DEFAULT_CATEGORIES
import ir.alidevcore.autocare.util.JalaliDateUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DashboardUiState(
    val vehicle: Vehicle? = null,
    val overdueItems: List<ItemWithLastRecord> = emptyList(),
    val upcomingItems: List<ItemWithLastRecord> = emptyList(),
    val allItems: List<MaintenanceItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val allRecords: List<RecordDisplay> = emptyList(),
    val recentRecords: List<RecordDisplay> = emptyList(),
    val pendingPlans: List<PlannedServiceDisplay> = emptyList(),
    val monthlyTotal: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val showKmUpdateSuccess: Boolean = false
)

data class RecordDisplay(
    val record: MaintenanceRecord,
    val itemName: String,
    val dateDisplay: String
)

data class PlannedServiceDisplay(
    val plan: PlannedService,
    val itemName: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: DatabaseBackupManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L

    private val _selectedTab = MutableStateFlow(
        savedStateHandle.get<String>("selectedTab")?.let {
            runCatching { DashboardTab.valueOf(it) }.getOrNull()
        } ?: DashboardTab.STATUS
    )
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: DashboardTab) {
        _selectedTab.value = tab
        savedStateHandle["selectedTab"] = tab.name
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refreshDashboard() {
        loadDashboard()
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadDashboard() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                vehicleRepository.getVehicleById(vehicleId).filterNotNull(),
                maintenanceRepository.getAllItemsForVehicle(vehicleId),
                maintenanceRepository.getPendingServicesForVehicle(vehicleId),
                maintenanceRepository.getRecordsForVehicle(vehicleId)
            ) { vehicle, allItemsList, pendingPlansList, allRecordsList ->
                // Compute item statuses
                val itemStatuses = maintenanceRepository.computeItemStatuses(vehicle.id, vehicle.currentKm)
                val overdueItems = itemStatuses.filter { it.status == ServiceStatus.OVERDUE }
                val upcomingItems = itemStatuses.filter { it.status == ServiceStatus.UPCOMING }

                // Map categories
                val customCats = allItemsList.map { it.category }.distinct()
                val categories = (DEFAULT_CATEGORIES + customCats).distinct()

                val itemMap = allItemsList.associateBy { it.id }

                // Map pending plans with item names
                val pendingPlans = pendingPlansList.map { plan ->
                    val item = itemMap[plan.itemId]
                    PlannedServiceDisplay(
                        plan = plan,
                        itemName = item?.name ?: "سرویس دوره‌ای"
                    )
                }

                // Map records sorted by KM DESCENDING (highest km to lowest)
                val sortedRecords = allRecordsList.sortedByDescending { it.km }
                val allRecords = sortedRecords.map { record ->
                    val item = itemMap[record.itemId]
                    RecordDisplay(
                        record = record,
                        itemName = item?.name ?: "نامشخص",
                        dateDisplay = record.date?.let { JalaliDateUtil.timestampToJalaliStringWithMonthName(it) } ?: ""
                    )
                }
                val recentRecords = allRecords.take(5)

                // Monthly cost
                val now = System.currentTimeMillis()
                val startOfMonth = JalaliDateUtil.run {
                    val (y, m, _) = getCurrentJalaliDate()
                    getStartOfJalaliMonth(y, m)
                }
                val monthlyTotal = maintenanceRepository.getTotalCostForVehicleInRange(vehicle.id, startOfMonth, now)

                DashboardUiState(
                    vehicle = vehicle,
                    overdueItems = overdueItems,
                    upcomingItems = upcomingItems,
                    allItems = allItemsList,
                    categories = categories,
                    allRecords = allRecords,
                    recentRecords = recentRecords,
                    pendingPlans = pendingPlans,
                    monthlyTotal = monthlyTotal,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun deletePlan(plan: PlannedService) {
        viewModelScope.launch {
            try {
                val oldReminders = reminderRepository.getRemindersForServiceOnce(plan.id)
                oldReminders.forEach { reminderScheduler.cancelReminder(it.notificationId) }
                reminderRepository.deleteRemindersForService(plan.id)
                maintenanceRepository.deletePlannedService(plan)
                
                // Immediately remove from uiState so UI updates immediately
                _uiState.update { current ->
                    current.copy(
                        pendingPlans = current.pendingPlans.filter { it.plan.id != plan.id }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در حذف برنامه: ${e.message}") }
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

    fun updateCurrentKm(newKm: Long) {
        viewModelScope.launch {
            vehicleRepository.updateCurrentKm(vehicleId, newKm)
            _uiState.update { it.copy(showKmUpdateSuccess = true) }
        }
    }

    fun clearKmUpdateSuccess() {
        _uiState.update { it.copy(showKmUpdateSuccess = false) }
    }

    fun addItem(
        name: String,
        category: String,
        intervalKm: Long?,
        intervalMonths: Int?,
        description: String
    ) {
        viewModelScope.launch {
            try {
                val item = MaintenanceItem(
                    vehicleId = vehicleId,
                    name = name.trim(),
                    category = category.trim(),
                    intervalKm = intervalKm,
                    intervalMonths = intervalMonths,
                    description = description.trim(),
                    isActive = true
                )
                maintenanceRepository.insertItem(item)
                _uiState.update { it.copy(message = "قلم با موفقیت اضافه شد") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در ثبت: ${e.message}") }
            }
        }
    }

    fun updateItem(item: MaintenanceItem) {
        viewModelScope.launch {
            try {
                maintenanceRepository.updateItem(item)
                _uiState.update { it.copy(message = "قلم با موفقیت ویرایش شد") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در ویرایش: ${e.message}") }
            }
        }
    }

    fun deleteItem(item: MaintenanceItem) {
        viewModelScope.launch {
            try {
                maintenanceRepository.deleteItem(item)
                _uiState.update { it.copy(message = "قلم با موفقیت حذف شد") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در حذف: ${e.message}") }
            }
        }
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            try {
                maintenanceRepository.deleteRecord(record)
                _uiState.update { it.copy(message = "سابقه سرویس با موفقیت حذف شد.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در حذف سابقه: ${e.message}") }
            }
        }
    }

    fun exportRecordsToCsv() {
        val state = _uiState.value
        val vehicleName = state.vehicle?.name ?: "خودرو"
        if (state.allRecords.isEmpty()) {
            _uiState.update { it.copy(error = "هیچ سابقه‌ای برای خروجی اکسل وجود ندارد.") }
            return
        }
        viewModelScope.launch {
            try {
                val recordsPair = state.allRecords.map { Pair(it.record, it.itemName) }
                val file = backupManager.exportRecordsToExcel(vehicleName, recordsPair)
                backupManager.shareExcelFile(file)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در خروجی اکسل: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
