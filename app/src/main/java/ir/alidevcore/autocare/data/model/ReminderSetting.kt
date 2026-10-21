package ir.alidevcore.autocare.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_settings",
    foreignKeys = [
        ForeignKey(
            entity = PlannedService::class,
            parentColumns = ["id"],
            childColumns = ["plannedServiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plannedServiceId")]
)
data class ReminderSetting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plannedServiceId: Long,
    val daysBefore: Int,       // چند روز قبل از موعد
    val isEnabled: Boolean = true,
    val notificationId: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
