package ir.alidevcore.autocare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,           // نام خودرو
    val currentKm: Long,        // کیلومتر فعلی
    val year: Int? = null,      // سال ساخت (اختیاری)
    val createdAt: Long = System.currentTimeMillis()  // timestamp
)
