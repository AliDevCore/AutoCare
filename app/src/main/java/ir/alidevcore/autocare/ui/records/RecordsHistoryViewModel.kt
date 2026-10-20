package ir.alidevcore.autocare.ui.records

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordWithItemInfo(
    val record: MaintenanceRecord,
    val itemName: String,
    val category: String
)

enum class SortOption {
    DATE_DESC,
    DATE_ASC,
    KM_DESC,
    KM_ASC
}

data class RecordsHistoryUiState(
    val records: List<RecordWithItemInfo> = emptyList(),
    val filteredRecords: List<RecordWithItemInfo> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val filterItemId: Long? = null,
    val filterItemName: String? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_DESC,
    val fromKm: Long? = null,
    val toKm: Long? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class RecordsHistoryViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L
    private val filterItemId: Long? = savedStateHandle.get<Long>("itemId")

    private val _uiState = MutableStateFlow(RecordsHistoryUiState(filterItemId = filterItemId))
    val uiState: StateFlow<RecordsHistoryUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            maintenanceRepository.getRecordsForVehicle(vehicleId)
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { records ->
                    val enriched = mutableListOf<RecordWithItemInfo>()
                    val categories = mutableSetOf<String>()

                    for (r in records) {
                        val item = maintenanceRepository.getItemByIdOnce(r.itemId)
                        val name = item?.name ?: "نامشخص"
                        val cat = item?.category ?: "عمومی"
                        categories.add(cat)
                        enriched.add(RecordWithItemInfo(record = r, itemName = name, category = cat))
                    }

                    var itemFilterName: String? = null
                    if (filterItemId != null && filterItemId > 0) {
                        itemFilterName = maintenanceRepository.getItemByIdOnce(filterItemId)?.name
                    }

                    _uiState.update {
                        it.copy(
                            records = enriched,
                            categories = categories.toList().sorted(),
                            filterItemName = itemFilterName,
                            isLoading = false
                        )
                    }
                    applyFilters()
                }
        }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        applyFilters()
    }

    fun onCategorySelected(cat: String?) {
        _uiState.update { it.copy(selectedCategory = cat) }
        applyFilters()
    }

    fun onSortOptionChanged(sort: SortOption) {
        _uiState.update { it.copy(sortOption = sort) }
        applyFilters()
    }

    fun setKmRange(from: Long?, to: Long?) {
        _uiState.update { it.copy(fromKm = from, toKm = to) }
        applyFilters()
    }

    fun setDateRange(from: Long?, to: Long?) {
        _uiState.update { it.copy(fromDate = from, toDate = to) }
        applyFilters()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedCategory = null,
                filterItemId = null,
                filterItemName = null,
                fromKm = null,
                toKm = null,
                fromDate = null,
                toDate = null
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var list = state.records

        // Item filter
        if (state.filterItemId != null && state.filterItemId > 0) {
            list = list.filter { it.record.itemId == state.filterItemId }
        }

        // Category filter
        if (state.selectedCategory != null) {
            list = list.filter { it.category == state.selectedCategory }
        }

        // Search query
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim()
            list = list.filter {
                it.itemName.contains(q, ignoreCase = true) ||
                        it.category.contains(q, ignoreCase = true) ||
                        it.record.description.contains(q, ignoreCase = true) ||
                        it.record.parts.contains(q, ignoreCase = true) ||
                        it.record.workshop.contains(q, ignoreCase = true)
            }
        }

        // Km range
        if (state.fromKm != null) {
            list = list.filter { it.record.km >= state.fromKm }
        }
        if (state.toKm != null) {
            list = list.filter { it.record.km <= state.toKm }
        }

        // Date range
        if (state.fromDate != null) {
            list = list.filter { it.record.date != null && it.record.date >= state.fromDate }
        }
        if (state.toDate != null) {
            list = list.filter { it.record.date != null && it.record.date <= state.toDate }
        }

        // Sorting
        list = when (state.sortOption) {
            SortOption.DATE_DESC -> list.sortedWith(
                compareByDescending<RecordWithItemInfo> { it.record.date ?: 0L }
                    .thenByDescending { it.record.km }
            )
            SortOption.DATE_ASC -> list.sortedWith(
                compareBy<RecordWithItemInfo> { it.record.date ?: Long.MAX_VALUE }
                    .thenBy { it.record.km }
            )
            SortOption.KM_DESC -> list.sortedByDescending { it.record.km }
            SortOption.KM_ASC -> list.sortedBy { it.record.km }
        }

        _uiState.update { it.copy(filteredRecords = list) }
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            try {
                maintenanceRepository.deleteRecord(record)
                _uiState.update { it.copy(message = "سابقه با موفقیت حذف شد") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطا در حذف سابقه: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
