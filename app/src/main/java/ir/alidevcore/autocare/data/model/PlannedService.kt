package ir.alidevcore.autocare.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlanStatus {
    PENDING,     // در انتظار
    DONE,        // انجام شده
    CANCELLED    // لغو شده
}

@Entity(
    tableName = "planned_services",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("vehicleId")]
)
data class PlannedService(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val vehicleId: Long,
    val targetKm: Long? = null,         // کیلومتر هدف
    val targetDate: Long? = null,       // تاریخ هدف (timestamp)
    val notes: String = "",
    val status: PlanStatus = PlanStatus.PENDING,
    val convertedToRecordId: Long? = null,  // if converted to a real record
    val createdAt: Long = System.currentTimeMillis()
)
