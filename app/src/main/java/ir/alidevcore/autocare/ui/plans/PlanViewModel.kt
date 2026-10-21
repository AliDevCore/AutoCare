package ir.alidevcore.autocare.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.PlanStatus
import ir.alidevcore.autocare.data.model.ReminderSetting
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.repository.ReminderRepository
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.notification.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddPlanUiState(
    val items: List<MaintenanceItem> = emptyList(),
    val selectedItem: MaintenanceItem? = null,
    val targetKmText: String = "",
    val targetDate: Long? = null,
    val notes: String = "",
    // Reminder options: 0 (same day), 1 (1 day before), 3 (3 days before), 7 (1 week before)
    val selectedReminderDays: Set<Int> = setOf(0, 1),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val reminderRepository: ReminderRepository,
    private val vehicleRepository: VehicleRepository,
    private val reminderScheduler: ReminderScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L
    private val rawItemId: Long? = savedStateHandle.get<Long>("itemId")
    private val initialItemId: Long? = if (rawItemId != null && rawItemId > 0) rawItemId else null
    private val rawPlanId: Long? = savedStateHandle.get<Long>("planId")
    val planId: Long? = if (rawPlanId != null && rawPlanId > 0) rawPlanId else null

    private val _uiState = MutableStateFlow(AddPlanUiState())
    val uiState: StateFlow<AddPlanUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val items = maintenanceRepository.getActiveItemsForVehicle(vehicleId).first()
            var selectedItem: MaintenanceItem? = null
            var km = ""
            var date: Long? = null
            var notes = ""
            val reminderDays = mutableSetOf(0, 1)

            if (planId != null && planId > 0) {
                val plan = maintenanceRepository.getServiceById(planId)
                if (plan != null) {
                    selectedItem = items.find { it.id == plan.itemId }
                    km = plan.targetKm?.toString() ?: ""
                    date = plan.targetDate
                    notes = plan.notes

                    val reminders = reminderRepository.getRemindersForServiceOnce(planId)
                    reminderDays.clear()
                    reminders.forEach { reminderDays.add(it.daysBefore) }
                }
            } else if (initialItemId != null && initialItemId > 0) {
                selectedItem = items.find { it.id == initialItemId }
            }

            _uiState.update {
                it.copy(
                    items = items,
                    selectedItem = selectedItem ?: items.firstOrNull(),
                    targetKmText = km,
                    targetDate = date,
                    notes = notes,
                    selectedReminderDays = reminderDays,
                    isEditing = (planId != null && planId > 0),
                    isLoading = false
                )
            }
        }
    }

    fun onItemSelected(item: MaintenanceItem) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun onTargetKmChanged(km: String) {
        _uiState.update { it.copy(targetKmText = km) }
    }

    fun onTargetDateChanged(timestamp: Long?) {
        _uiState.update { it.copy(targetDate = timestamp) }
    }

    fun toggleReminderDay(daysBefore: Int) {
        _uiState.update { state ->
            val set = state.selectedReminderDays.toMutableSet()
            if (set.contains(daysBefore)) {
                set.remove(daysBefore)
            } else {
                set.add(daysBefore)
            }
            state.copy(selectedReminderDays = set)
        }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun savePlan() {
        val state = _uiState.value
        val item = state.selectedItem
        if (item == null) {
            _uiState.update { it.copy(error = "لطفاً قلم سرویس را انتخاب کنید") }
            return
        }

        val km = state.targetKmText.trim().let { if (it.isNotBlank()) it.toLongOrNull() else null }
        val date = state.targetDate

        if (km == null && date == null) {
            _uiState.update { it.copy(error = "حداقل یکی از موارد کیلومتر هدف یا تاریخ هدف را مشخص کنید") }
            return
        }

        viewModelScope.launch {
            try {
                val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId)
                val vehicleName = vehicle?.name ?: "خودرو"

                val plan = PlannedService(
                    id = if (state.isEditing && planId != null && planId > 0) planId else 0L,
                    itemId = item.id,
                    vehicleId = vehicleId,
                    targetKm = km,
                    targetDate = date,
                    notes = state.notes.trim(),
                    status = PlanStatus.PENDING
                )

                val savedPlanId = if (state.isEditing) {
                    maintenanceRepository.updatePlannedService(plan)
                    // Cancel old alarms
                    val oldReminders = reminderRepository.getRemindersForServiceOnce(plan.id)
                    oldReminders.forEach { reminderScheduler.cancelReminder(it.notificationId) }
                    reminderRepository.deleteRemindersForService(plan.id)
                    plan.id
                } else {
                    maintenanceRepository.insertPlannedService(plan)
                }

                // If targetDate is specified and reminders are enabled, schedule alarms
                if (date != null && state.selectedReminderDays.isNotEmpty()) {
                    val reminders = mutableListOf<ReminderSetting>()
                    for (daysBefore in state.selectedReminderDays) {
                        val notifId = (savedPlanId * 100 + daysBefore).toInt()
                        val reminder = ReminderSetting(
                            plannedServiceId = savedPlanId,
                            daysBefore = daysBefore,
                            isEnabled = true,
                            notificationId = notifId
                        )
                        reminders.add(reminder)

                        val triggerTime = reminderScheduler.computeTriggerTime(date, daysBefore)
                        if (triggerTime > System.currentTimeMillis()) {
                            reminderScheduler.scheduleReminder(
                                notificationId = notifId,
                                triggerAtMs = triggerTime,
                                vehicleName = vehicleName,
                                serviceName = item.name,
                                vehicleId = vehicleId,
                                daysUntilDue = daysBefore
                            )
                        }
                    }
                    reminderRepository.insertReminders(reminders)
                }

                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در ذخیره برنامه: ${e.message}") }
            }
        }
    }

    fun deletePlan() {
        val id = planId ?: return
        viewModelScope.launch {
            try {
                val oldReminders = reminderRepository.getRemindersForServiceOnce(id)
                oldReminders.forEach { reminderScheduler.cancelReminder(it.notificationId) }
                reminderRepository.deleteRemindersForService(id)
                val plan = maintenanceRepository.getServiceById(id)
                if (plan != null) {
                    maintenanceRepository.deletePlannedService(plan)
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در حذف برنامه: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
