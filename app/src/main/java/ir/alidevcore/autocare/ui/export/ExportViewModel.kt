package ir.alidevcore.autocare.ui.export

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.util.ExcelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ExportUiState(
    val vehicle: Vehicle? = null,
    val allVehiclesCount: Int = 0,
    val isExporting: Boolean = false,
    val exportedFile: File? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId)
            val count = vehicleRepository.getVehicleCount()
            _uiState.update { it.copy(vehicle = vehicle, allVehiclesCount = count) }
        }
    }

    fun exportCurrentVehicleExcel(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                val file = withContext(Dispatchers.IO) {
                    val exportData = buildVehicleExportData(vehicleId)
                    ExcelExporter.exportVehicleToExcel(context, exportData)
                }
                _uiState.update { it.copy(isExporting = false, exportedFile = file, message = "فایل اکسل با موفقیت ایجاد شد") }
                ExcelExporter.shareExportedFile(context, file)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = "خطا در تولید فایل اکسل: ${e.message}") }
            }
        }
    }

    fun exportCurrentVehicleCsv(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                val file = withContext(Dispatchers.IO) {
                    val exportData = buildVehicleExportData(vehicleId)
                    ExcelExporter.exportVehicleToCsv(context, exportData)
                }
                _uiState.update { it.copy(isExporting = false, exportedFile = file, message = "فایل CSV با موفقیت ایجاد شد") }
                ExcelExporter.shareExportedFile(context, file, "text/csv")
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = "خطا در تولید CSV: ${e.message}") }
            }
        }
    }

    fun exportAllVehiclesExcel(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                val file = withContext(Dispatchers.IO) {
                    val vehicles = vehicleRepository.getAllVehicles().first()
                    val allExportData = vehicles.map { v ->
                        buildVehicleExportData(v.id)
                    }
                    ExcelExporter.exportAllVehiclesToExcel(context, allExportData)
                }
                _uiState.update { it.copy(isExporting = false, exportedFile = file, message = "فایل جامع همه خودروها ایجاد شد") }
                ExcelExporter.shareExportedFile(context, file)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = "خطا در تولید فایل: ${e.message}") }
            }
        }
    }

    private suspend fun buildVehicleExportData(vId: Long): ExcelExporter.VehicleExportData {
        val v = vehicleRepository.getVehicleByIdOnce(vId) ?: throw IllegalStateException("خودرو یافت نشد")
        val items = maintenanceRepository.getAllItemsForVehicle(vId).first()
        val records = maintenanceRepository.getRecordsForVehicle(vId).first()
        val plans = maintenanceRepository.getAllServicesForVehicle(vId).first()

        val recordPairs = records.map { r ->
            val itemName = items.find { it.id == r.itemId }?.name ?: "سایر"
            Pair(r, itemName)
        }

        val planPairs = plans.map { p ->
            val itemName = items.find { it.id == p.itemId }?.name ?: "سایر"
            Pair(p, itemName)
        }

        return ExcelExporter.VehicleExportData(
            vehicle = v,
            items = items,
            records = recordPairs,
            plans = planPairs
        )
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
