package ir.alidevcore.autocare.data.model.relations

data class VehicleWithStats(
    val vehicleId: Long,
    val vehicleName: String,
    val currentKm: Long,
    val year: Int?,
    val overdueCount: Int,
    val upcomingCount: Int
)
