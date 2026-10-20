package ir.alidevcore.autocare.ui.records

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.util.JalaliDateUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NextScheduleMode {
    NONE,
    ONLY_KM,
    ONLY_DATE,
    BOTH
}

data class AddRecordUiState(
    val vehicle: Vehicle? = null,
    val items: List<MaintenanceItem> = emptyList(),
    val selectedItem: MaintenanceItem? = null,
    val date: Long = System.currentTimeMillis(),
    val isDateUnknown: Boolean = false,
    val kmText: String = "",
    val costText: String = "",
    val description: String = "",
    val parts: String = "",
    val workshop: String = "",
    val nextScheduleMode: NextScheduleMode = NextScheduleMode.NONE,
    val nextKmText: String = "",
    val nextDate: Long? = null,
    val planId: Long? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L
    private val initialItemId: Long? = savedStateHandle.get<Long>("itemId")
    private val recordId: Long? = savedStateHandle.get<Long>("recordId")
    private val planId: Long? = savedStateHandle.get<Long>("planId")

    private val _uiState = MutableStateFlow(AddRecordUiState(planId = planId))
    val uiState: StateFlow<AddRecordUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId)
            val items = maintenanceRepository.getActiveItemsForVehicle(vehicleId).first()

            var selectedItem: MaintenanceItem? = null
            var defaultDate = System.currentTimeMillis()
            var isDateUnknown = false
            var defaultKm = vehicle?.currentKm?.toString() ?: ""
            var desc = ""
            var parts = ""
            var workshop = ""
            var cost = ""

            // Check if editing an existing record
            if (recordId != null && recordId > 0) {
                val record = maintenanceRepository.getRecordById(recordId)
                if (record != null) {
                    selectedItem = items.find { it.id == record.itemId }
                    if (record.date != null) {
                        defaultDate = record.date
                        isDateUnknown = false
                    } else {
                        isDateUnknown = true
                    }
                    defaultKm = record.km.toString()
                    cost = record.cost?.toString() ?: ""
                    desc = record.description
                    parts = record.parts
                    workshop = record.workshop
                }
            } else if (planId != null && planId > 0) {
                // Converting from planned service
                val plan = maintenanceRepository.getServiceById(planId)
                if (plan != null) {
                    selectedItem = items.find { it.id == plan.itemId }
                    if (plan.targetKm != null) defaultKm = plan.targetKm.toString()
                    if (plan.targetDate != null) defaultDate = plan.targetDate
                    desc = plan.notes
                }
            } else if (initialItemId != null && initialItemId > 0) {
                selectedItem = items.find { it.id == initialItemId }
            }

            // Auto-compute next schedule if item has intervals
            var nextMode = NextScheduleMode.NONE
            var nextKm = ""
            var nextDateTimestamp: Long? = null

            val item = selectedItem ?: items.firstOrNull()
            if (item != null) {
                val currentKmVal = defaultKm.toLongOrNull() ?: vehicle?.currentKm ?: 0L
                if (item.intervalKm != null && item.intervalMonths != null) {
                    nextMode = NextScheduleMode.BOTH
                    nextKm = (currentKmVal + item.intervalKm).toString()
                    nextDateTimestamp = JalaliDateUtil.addMonthsToTimestamp(defaultDate, item.intervalMonths)
                } else if (item.intervalKm != null) {
                    nextMode = NextScheduleMode.ONLY_KM
                    nextKm = (currentKmVal + item.intervalKm).toString()
                } else if (item.intervalMonths != null) {
                    nextMode = NextScheduleMode.ONLY_DATE
                    nextDateTimestamp = JalaliDateUtil.addMonthsToTimestamp(defaultDate, item.intervalMonths)
                }
            }

            _uiState.update {
                it.copy(
                    vehicle = vehicle,
                    items = items,
                    selectedItem = item,
                    date = defaultDate,
                    isDateUnknown = isDateUnknown,
                    kmText = defaultKm,
                    costText = cost,
                    description = desc,
                    parts = parts,
                    workshop = workshop,
                    nextScheduleMode = nextMode,
                    nextKmText = nextKm,
                    nextDate = nextDateTimestamp,
                    isEditing = (recordId != null && recordId > 0),
                    isLoading = false
                )
            }
        }
    }

    fun onItemSelected(item: MaintenanceItem) {
        val currentKm = _uiState.value.kmText.toLongOrNull() ?: _uiState.value.vehicle?.currentKm ?: 0L
        val currentDate = _uiState.value.date

        var nextMode = _uiState.value.nextScheduleMode
        var nextKm = _uiState.value.nextKmText
        var nextDateTimestamp = _uiState.value.nextDate

        if (item.intervalKm != null && item.intervalMonths != null) {
            nextMode = NextScheduleMode.BOTH
            nextKm = (currentKm + item.intervalKm).toString()
            nextDateTimestamp = JalaliDateUtil.addMonthsToTimestamp(currentDate, item.intervalMonths)
        } else if (item.intervalKm != null) {
            nextMode = NextScheduleMode.ONLY_KM
            nextKm = (currentKm + item.intervalKm).toString()
        } else if (item.intervalMonths != null) {
            nextMode = NextScheduleMode.ONLY_DATE
            nextDateTimestamp = JalaliDateUtil.addMonthsToTimestamp(currentDate, item.intervalMonths)
        }

        _uiState.update {
            it.copy(
                selectedItem = item,
                nextScheduleMode = nextMode,
                nextKmText = nextKm,
                nextDate = nextDateTimestamp
            )
        }
    }

    fun onDateChanged(timestamp: Long) {
        _uiState.update { state ->
            val updatedNextDate = state.selectedItem?.intervalMonths?.let { months ->
                JalaliDateUtil.addMonthsToTimestamp(timestamp, months)
            } ?: state.nextDate
            state.copy(date = timestamp, nextDate = updatedNextDate)
        }
    }

    fun onToggleDateUnknown(unknown: Boolean) {
        _uiState.update { state ->
            val updatedMode = if (unknown && state.nextScheduleMode == NextScheduleMode.BOTH) {
                NextScheduleMode.ONLY_KM
            } else if (unknown && state.nextScheduleMode == NextScheduleMode.ONLY_DATE) {
                NextScheduleMode.NONE
            } else state.nextScheduleMode

            val updatedNextDate = if (unknown) null else state.selectedItem?.intervalMonths?.let { months ->
                JalaliDateUtil.addMonthsToTimestamp(state.date, months)
            } ?: state.nextDate

            state.copy(
                isDateUnknown = unknown,
                nextScheduleMode = updatedMode,
                nextDate = updatedNextDate
            )
        }
    }

    fun onKmChanged(km: String) {
        _uiState.update { state ->
            val parsed = km.toLongOrNull()
            val nextKm = if (parsed != null && state.selectedItem?.intervalKm != null) {
                (parsed + state.selectedItem.intervalKm).toString()
            } else state.nextKmText
            state.copy(kmText = km, nextKmText = nextKm)
        }
    }

    fun onCostChanged(cost: String) {
        _uiState.update { it.copy(costText = cost) }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun onPartsChanged(parts: String) {
        _uiState.update { it.copy(parts = parts) }
    }

    fun onWorkshopChanged(workshop: String) {
        _uiState.update { it.copy(workshop = workshop) }
    }

    fun onNextScheduleModeChanged(mode: NextScheduleMode) {
        _uiState.update { it.copy(nextScheduleMode = mode) }
    }

    fun onNextKmChanged(km: String) {
        _uiState.update { it.copy(nextKmText = km) }
    }

    fun onNextDateChanged(timestamp: Long) {
        _uiState.update { it.copy(nextDate = timestamp) }
    }

    fun saveRecord(updateVehicleKm: Boolean) {
        val state = _uiState.value
        val item = state.selectedItem
        if (item == null) {
            _uiState.update { it.copy(error = "لطفاً قلم تعمیراتی یا سرویس را انتخاب کنید") }
            return
        }

        val km = state.kmText.toLongOrNull()
        if (km == null || km < 0) {
            _uiState.update { it.copy(error = "کیلومتر واردشده نامعتبر است") }
            return
        }

        val cost = state.costText.trim().let { if (it.isNotBlank()) it.toLongOrNull() else null }

        viewModelScope.launch {
            try {
                val record = MaintenanceRecord(
                    id = recordId ?: 0L,
                    itemId = item.id,
                    date = if (state.isDateUnknown) null else state.date,
                    km = km,
                    cost = cost,
                    description = state.description.trim(),
                    parts = state.parts.trim(),
                    workshop = state.workshop.trim()
                )

                val savedRecordId = if (state.isEditing) {
                    maintenanceRepository.updateRecord(record)
                    record.id
                } else {
                    maintenanceRepository.insertRecord(record)
                }

                // If converted from a plan, mark plan as DONE
                if (planId != null && planId > 0) {
                    maintenanceRepository.markPlanAsDone(planId, savedRecordId)
                }

                // If user confirmed updating vehicle current km
                if (updateVehicleKm) {
                    vehicleRepository.updateCurrentKm(vehicleId, km)
                }

                // Create planned service for next turn if scheduled
                if (!state.isEditing && state.nextScheduleMode != NextScheduleMode.NONE) {
                    val nextKmVal = if (state.nextScheduleMode in listOf(NextScheduleMode.ONLY_KM, NextScheduleMode.BOTH)) {
                        state.nextKmText.toLongOrNull()
                    } else null

                    val nextDateVal = if (state.nextScheduleMode in listOf(NextScheduleMode.ONLY_DATE, NextScheduleMode.BOTH)) {
                        state.nextDate
                    } else null

                    if (nextKmVal != null || nextDateVal != null) {
                        val nextPlan = PlannedService(
                            itemId = item.id,
                            vehicleId = vehicleId,
                            targetKm = nextKmVal,
                            targetDate = nextDateVal,
                            notes = "محاسبه خودکار از سرویس قبلی (${item.name})"
                        )
                        maintenanceRepository.insertPlannedService(nextPlan)
                    }
                }

                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در ذخیره سابقه: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
