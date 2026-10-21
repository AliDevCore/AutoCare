package ir.alidevcore.autocare.data.model.relations

data class ItemWithLastRecord(
    val itemId: Long,
    val vehicleId: Long,
    val itemName: String,
    val category: String,
    val intervalKm: Long?,
    val intervalMonths: Int?,
    val isActive: Boolean,
    val lastRecordDate: Long?,
    val lastRecordKm: Long?,
    // Next service info
    val nextKm: Long?,
    val nextDate: Long?,
    // Computed status
    val status: ServiceStatus
)

enum class ServiceStatus {
    OVERDUE,    // عقب‌افتاده
    UPCOMING,   // نزدیک به موعد
    OK,         // عادی
    NO_SCHEDULE // بدون برنامه
}
