package ir.alidevcore.autocare.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.dao.CategoryCost
import ir.alidevcore.autocare.data.dao.MonthlyCost
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.util.JalaliDateUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExpensePeriod {
    ALL_TIME,
    THIS_YEAR,
    THIS_MONTH
}

data class MonthlyBarData(
    val label: String,
    val amount: Long,
    val count: Int
)

data class ExpensesUiState(
    val vehicle: Vehicle? = null,
    val period: ExpensePeriod = ExpensePeriod.ALL_TIME,
    val totalCost: Long = 0L,
    val recordsWithCostCount: Int = 0,
    val totalRecordsCount: Int = 0,
    val categoryCosts: List<CategoryCost> = emptyList(),
    val monthlyBars: List<MonthlyBarData> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        loadExpenses(ExpensePeriod.ALL_TIME)
    }

    fun onPeriodChanged(period: ExpensePeriod) {
        _uiState.update { it.copy(period = period, isLoading = true) }
        loadExpenses(period)
    }

    private fun loadExpenses(period: ExpensePeriod) {
        viewModelScope.launch {
            try {
                val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId)
                val allRecords = maintenanceRepository.getRecordsForVehicle(vehicleId).first()

                val (currentYear, currentMonth, _) = JalaliDateUtil.getCurrentJalaliDate()
                val now = System.currentTimeMillis()

                val fromDate = when (period) {
                    ExpensePeriod.ALL_TIME -> 0L
                    ExpensePeriod.THIS_YEAR -> JalaliDateUtil.getStartOfCurrentJalaliYear()
                    ExpensePeriod.THIS_MONTH -> JalaliDateUtil.getStartOfJalaliMonth(currentYear, currentMonth)
                }

                val periodRecords = allRecords.filter { rec ->
                    if (period == ExpensePeriod.ALL_TIME) true
                    else rec.date != null && rec.date in fromDate..now
                }
                val costedRecords = periodRecords.filter { it.cost != null }
                val total = costedRecords.sumOf { it.cost!! }

                // Category breakdown
                val catMap = mutableMapOf<String, Long>()
                val catCountMap = mutableMapOf<String, Int>()

                for (rec in costedRecords) {
                    val item = maintenanceRepository.getItemByIdOnce(rec.itemId)
                    val cat = item?.category ?: "عمومی"
                    catMap[cat] = (catMap[cat] ?: 0L) + (rec.cost ?: 0L)
                    catCountMap[cat] = (catCountMap[cat] ?: 0) + 1
                }

                val catCosts = catMap.map { (cat, cost) ->
                    CategoryCost(category = cat, totalCost = cost, recordCount = catCountMap[cat] ?: 0)
                }.sortedByDescending { it.totalCost }

                // Monthly breakdown (last 6 Jalali months)
                val monthlyData = mutableListOf<MonthlyBarData>()
                for (i in 5 downTo 0) {
                    var m = currentMonth - i
                    var y = currentYear
                    while (m < 1) {
                        m += 12
                        y -= 1
                    }
                    val mStart = JalaliDateUtil.getStartOfJalaliMonth(y, m)
                    val mEnd = JalaliDateUtil.getEndOfJalaliMonth(y, m)

                    val mRecs = allRecords.filter { it.date != null && it.date in mStart..mEnd && it.cost != null }
                    val mTotal = mRecs.sumOf { it.cost!! }
                    val mName = JalaliDateUtil.getJalaliMonthName(m)

                    monthlyData.add(MonthlyBarData(label = mName, amount = mTotal, count = mRecs.size))
                }

                _uiState.update {
                    it.copy(
                        vehicle = vehicle,
                        totalCost = total,
                        recordsWithCostCount = costedRecords.size,
                        totalRecordsCount = periodRecords.size,
                        categoryCosts = catCosts,
                        monthlyBars = monthlyData,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
