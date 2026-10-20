package ir.alidevcore.autocare.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_items",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class MaintenanceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val name: String,                   // نام قطعه/سرویس
    val category: String,               // دسته‌بندی
    val description: String = "",       // توضیحات
    val intervalKm: Long? = null,       // فاصله کیلومتری
    val intervalMonths: Int? = null,    // فاصله ماهانه
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
