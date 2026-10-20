package ir.alidevcore.autocare.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val date: Long,             // timestamp of service date (stored as epoch ms, represents the date)
    val km: Long,               // odometer at service time
    val cost: Long? = null,     // هزینه (تومان) - اختیاری
    val description: String = "",
    val parts: String = "",     // مشخصات قطعه/مواد مصرفی
    val workshop: String = "",  // نام تعمیرگاه
    val createdAt: Long = System.currentTimeMillis()
)
