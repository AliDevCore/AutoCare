package ir.alidevcore.autocare.data.db

import androidx.room.TypeConverter
import ir.alidevcore.autocare.data.model.PlanStatus

class Converters {
    @TypeConverter
    fun fromPlanStatus(status: PlanStatus): String = status.name

    @TypeConverter
    fun toPlanStatus(value: String): PlanStatus = PlanStatus.valueOf(value)
}
