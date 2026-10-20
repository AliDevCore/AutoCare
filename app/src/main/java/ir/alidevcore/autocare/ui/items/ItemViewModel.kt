package ir.alidevcore.autocare.ui.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItemUiState(
    val items: List<MaintenanceItem> = emptyList(),
    val filteredItems: List<MaintenanceItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

val DEFAULT_CATEGORIES = listOf(
    "روغن‌ها و روانکارها",
    "فیلترها",
    "موتور",
    "گیربکس و انتقال قدرت",
    "سیستم ترمز",
    "سیستم تعلیق و جلوبندی",
    "برق و باتری",
    "سیستم خنک‌کننده",
    "لاستیک و چرخ",
    "بدنه و تجهیزات",
    "سرویس‌های عمومی",
    "تعمیرات متفرقه"
)

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L

    private val _uiState = MutableStateFlow(ItemUiState())
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            maintenanceRepository.getAllItemsForVehicle(vehicleId)
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { items ->
                    val customCats = items.map { it.category }.distinct()
                    val allCats = (DEFAULT_CATEGORIES + customCats).distinct()
                    _uiState.update { state ->
                        state.copy(
                            items = items,
                            categories = allCats,
                            isLoading = false
                        )
                    }
                    applyFilters()
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.items.filter { item ->
            val matchesCategory = state.selectedCategory == null || item.category == state.selectedCategory
            val matchesSearch = state.searchQuery.isBlank() ||
                    item.name.contains(state.searchQuery, ignoreCase = true) ||
                    item.category.contains(state.searchQuery, ignoreCase = true) ||
                    item.description.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        _uiState.update { it.copy(filteredItems = filtered) }
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

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
