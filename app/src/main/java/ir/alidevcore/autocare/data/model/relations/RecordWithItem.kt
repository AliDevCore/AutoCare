package ir.alidevcore.autocare.data.model.relations

import ir.alidevcore.autocare.data.model.MaintenanceRecord

data class RecordWithItem(
    val record: MaintenanceRecord,
    val itemName: String,
    val category: String,
    val vehicleId: Long
)
