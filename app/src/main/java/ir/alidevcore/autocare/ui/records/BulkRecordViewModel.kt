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
import ir.alidevcore.autocare.util.NumberUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BulkRowItem(
    val id: String = UUID.randomUUID().toString(),
    val kmText: String = "",
    val date: Long? = null,
    val costText: String = "",
    val description: String = ""
)

data class BulkRecordUiState(
    val vehicle: Vehicle? = null,
    val items: List<MaintenanceItem> = emptyList(),
    val selectedItem: MaintenanceItem? = null,
    val quickKmInput: String = "",
    val rows: List<BulkRowItem> = emptyList(),
    val updateVehicleKm: Boolean = true,
    val createNextPlan: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val savedCount: Int = 0,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class BulkRecordViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L
    private val initialItemId: Long? = savedStateHandle.get<Long>("itemId")

    private val _uiState = MutableStateFlow(BulkRecordUiState())
    val uiState: StateFlow<BulkRecordUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId)
            val items = maintenanceRepository.getActiveItemsForVehicle(vehicleId).first()

            val selectedItem = if (initialItemId != null && initialItemId > 0L) {
                items.find { it.id == initialItemId } ?: items.firstOrNull()
            } else {
                items.firstOrNull()
            }

            _uiState.update {
                it.copy(
                    vehicle = vehicle,
                    items = items,
                    selectedItem = selectedItem,
                    isLoading = false
                )
            }
        }
    }

    fun onItemSelected(item: MaintenanceItem) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun onQuickKmInputChanged(text: String) {
        _uiState.update { it.copy(quickKmInput = text) }
    }

    /**
     * Parses input like "30000, 60000, 90000, 130000" or "30k 60k 90k 130k" or multi-line strings
     */
    fun parseAndAddFromQuickInput() {
        val input = _uiState.value.quickKmInput.trim()
        if (input.isBlank()) return

        val parsedKms = parseKmList(input)
        if (parsedKms.isEmpty()) {
            _uiState.update { it.copy(error = "کیلومتر معتبری در متن واردشده شناسایی نشد. لطفاً از اعدادی مانند 30000, 60000, 90000 استفاده کنید.") }
            return
        }

        val currentRows = _uiState.value.rows.toMutableList()
        val existingKms = currentRows.mapNotNull { NumberUtil.parsePersianNumber(it.kmText) }.toSet()

        var addedCount = 0
        for (km in parsedKms) {
            if (!existingKms.contains(km)) {
                currentRows.add(
                    BulkRowItem(
                        kmText = km.toString(),
                        date = null,
                        costText = "",
                        description = ""
                    )
                )
                addedCount++
            }
        }

        // Sort rows by kilometer ascending
        val sortedRows = currentRows.sortedBy { NumberUtil.parsePersianNumber(it.kmText) ?: Long.MAX_VALUE }

        _uiState.update {
            it.copy(
                rows = sortedRows,
                quickKmInput = "",
                message = "تعداد $addedCount ردیف سابقه با موفقیت اضافه شد."
            )
        }
    }

    private fun parseKmList(input: String): List<Long> {
        val normalized = input
            .replace('۰', '0').replace('۱', '1').replace('۲', '2')
            .replace('۳', '3').replace('۴', '4').replace('۵', '5')
            .replace('۶', '6').replace('۷', '7').replace('۸', '8')
            .replace('۹', '9')
            .replace("هزار", "000")
            .replace("k", "000", ignoreCase = true)

        // Split on comma, Persian comma, semicolons, newlines, slashes, dashes, spaces
        val tokens = normalized.split(Regex("[,،;\\n\\r\\t\\/\\|\\-\\s]+"))

        return tokens.mapNotNull { token ->
            val digitsOnly = token.replace(Regex("[^0-9]"), "")
            digitsOnly.toLongOrNull()
        }.filter { it > 0 }
            .distinct()
            .sorted()
    }

    fun addNewEmptyRow() {
        _uiState.update { state ->
            val newRows = state.rows + BulkRowItem()
            state.copy(rows = newRows)
        }
    }

    fun removeRow(rowId: String) {
        _uiState.update { state ->
            state.copy(rows = state.rows.filterNot { it.id == rowId })
        }
    }

    fun clearAllRows() {
        _uiState.update { it.copy(rows = emptyList()) }
    }

    fun updateRowKm(rowId: String, km: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.id == rowId) row.copy(kmText = km) else row
                }
            )
        }
    }

    fun updateRowDate(rowId: String, date: Long?) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.id == rowId) row.copy(date = date) else row
                }
            )
        }
    }

    fun updateRowCost(rowId: String, cost: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.id == rowId) row.copy(costText = cost) else row
                }
            )
        }
    }

    fun updateRowDescription(rowId: String, desc: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.id == rowId) row.copy(description = desc) else row
                }
            )
        }
    }

    fun toggleUpdateVehicleKm(update: Boolean) {
        _uiState.update { it.copy(updateVehicleKm = update) }
    }

    fun toggleCreateNextPlan(create: Boolean) {
        _uiState.update { it.copy(createNextPlan = create) }
    }

    fun saveAllRecords() {
        val state = _uiState.value
        val item = state.selectedItem
        if (item == null) {
            _uiState.update { it.copy(error = "لطفاً ابتدا قلم سرویس مورد نظر را انتخاب کنید.") }
            return
        }

        if (state.rows.isEmpty()) {
            _uiState.update { it.copy(error = "هیچ ردیفی برای ثبت وارد نشده است. از جعبه ورود سریع یا دکمه افزودن ردیف استفاده کنید.") }
            return
        }

        val recordsToInsert = mutableListOf<MaintenanceRecord>()
        for ((index, row) in state.rows.withIndex()) {
            val km = NumberUtil.parsePersianNumber(row.kmText)
            if (km == null || km <= 0) {
                _uiState.update { it.copy(error = "کیلومتر ردیف شماره ${index + 1} نامعتبر است.") }
                return
            }

            val cost = if (row.costText.isNotBlank()) NumberUtil.parsePersianNumber(row.costText) else null

            recordsToInsert.add(
                MaintenanceRecord(
                    itemId = item.id,
                    date = row.date,
                    km = km,
                    cost = cost,
                    description = row.description.trim(),
                    parts = "",
                    workshop = ""
                )
            )
        }

        // Sort records by km ascending so historical records are stored chronologically
        recordsToInsert.sortBy { it.km }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                maintenanceRepository.insertRecords(recordsToInsert)

                val maxKm = recordsToInsert.maxOf { it.km }

                // 1. Update vehicle odometer if requested and higher than current
                if (state.updateVehicleKm && state.vehicle != null && maxKm > state.vehicle.currentKm) {
                    vehicleRepository.updateCurrentKm(vehicleId, maxKm)
                }

                // 2. Automatically create next planned service if requested and item has km interval
                if (state.createNextPlan && item.intervalKm != null && item.intervalKm > 0) {
                    val nextKm = maxKm + item.intervalKm
                    val nextPlan = PlannedService(
                        itemId = item.id,
                        vehicleId = vehicleId,
                        targetKm = nextKm,
                        targetDate = null,
                        notes = ""
                    )
                    maintenanceRepository.insertPlannedService(nextPlan)
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaved = true,
                        savedCount = recordsToInsert.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "خطا در ثبت سوابق: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
