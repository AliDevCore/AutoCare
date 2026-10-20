package ir.alidevcore.autocare.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.alidevcore.autocare.data.dao.*
import ir.alidevcore.autocare.data.db.AutoCareDatabase
import ir.alidevcore.autocare.data.model.*
import ir.alidevcore.autocare.util.JalaliDateUtil
import ir.alidevcore.autocare.worker.RescheduleRemindersWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class BackupStats(
    val vehicleCount: Int,
    val itemCount: Int,
    val recordCount: Int,
    val planCount: Int,
    val reminderCount: Int
)

@Singleton
class DatabaseBackupManager @Inject constructor(
    private val db: AutoCareDatabase,
    private val vehicleDao: VehicleDao,
    private val itemDao: MaintenanceItemDao,
    private val recordDao: MaintenanceRecordDao,
    private val plannedDao: PlannedServiceDao,
    private val reminderDao: ReminderSettingDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Exports the entire application database into a portable JSON backup file.
     */
    suspend fun exportDatabase(): File = withContext(Dispatchers.IO) {
        val vehicles = vehicleDao.getAllVehiclesList()
        val items = itemDao.getAllItemsList()
        val records = recordDao.getAllRecordsList()
        val plans = plannedDao.getAllServicesList()
        val reminders = reminderDao.getAllReminders()

        val root = JSONObject().apply {
            put("appName", "AutoCare")
            put("schemaVersion", 2)
            put("exportTimestamp", System.currentTimeMillis())

            // 1. Vehicles
            val vArray = JSONArray()
            vehicles.forEach { v ->
                vArray.put(JSONObject().apply {
                    put("id", v.id)
                    put("name", v.name)
                    put("currentKm", v.currentKm)
                    if (v.year != null) put("year", v.year) else put("year", JSONObject.NULL)
                    put("createdAt", v.createdAt)
                })
            }
            put("vehicles", vArray)

            // 2. Maintenance Items
            val iArray = JSONArray()
            items.forEach { it ->
                iArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("vehicleId", it.vehicleId)
                    put("name", it.name)
                    put("category", it.category)
                    if (it.intervalKm != null) put("intervalKm", it.intervalKm) else put("intervalKm", JSONObject.NULL)
                    if (it.intervalMonths != null) put("intervalMonths", it.intervalMonths) else put("intervalMonths", JSONObject.NULL)
                    put("description", it.description)
                    put("isActive", it.isActive)
                    put("createdAt", it.createdAt)
                })
            }
            put("items", iArray)

            // 3. Maintenance Records
            val rArray = JSONArray()
            records.forEach { r ->
                rArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("itemId", r.itemId)
                    if (r.date != null) put("date", r.date) else put("date", JSONObject.NULL)
                    put("km", r.km)
                    if (r.cost != null) put("cost", r.cost) else put("cost", JSONObject.NULL)
                    put("description", r.description)
                    put("parts", r.parts)
                    put("workshop", r.workshop)
                    if (r.invoiceUri != null) put("invoiceUri", r.invoiceUri) else put("invoiceUri", JSONObject.NULL)
                    put("createdAt", r.createdAt)
                })
            }
            put("records", rArray)

            // 4. Planned Services
            val pArray = JSONArray()
            plans.forEach { p ->
                pArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("itemId", p.itemId)
                    put("vehicleId", p.vehicleId)
                    if (p.targetKm != null) put("targetKm", p.targetKm) else put("targetKm", JSONObject.NULL)
                    if (p.targetDate != null) put("targetDate", p.targetDate) else put("targetDate", JSONObject.NULL)
                    put("notes", p.notes)
                    put("status", p.status.name)
                    if (p.convertedToRecordId != null) put("convertedToRecordId", p.convertedToRecordId) else put("convertedToRecordId", JSONObject.NULL)
                    put("createdAt", p.createdAt)
                })
            }
            put("plans", pArray)

            // 5. Reminders
            val remArray = JSONArray()
            reminders.forEach { rem ->
                remArray.put(JSONObject().apply {
                    put("id", rem.id)
                    put("plannedServiceId", rem.plannedServiceId)
                    put("daysBefore", rem.daysBefore)
                    put("isEnabled", rem.isEnabled)
                    put("notificationId", rem.notificationId)
                    put("createdAt", rem.createdAt)
                })
            }
            put("reminders", remArray)
        }

        val (year, month, day) = JalaliDateUtil.getCurrentJalaliDate()
        val timeStr = SimpleDateFormat("HHmmss", Locale.US).format(Date())
        val fileName = "AutoCare_Backup_${String.format("%04d%02d%02d", year, month, day)}_$timeStr.json"

        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(backupDir, fileName)

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(root.toString(2))
            }
        }
        file
    }

    /**
     * Opens the standard Android Share sheet so user can save or send the backup file.
     */
    fun shareBackupFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "ذخیره یا ارسال فایل پشتیبان دیتابیس").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Imports and replaces the entire database from a backup JSON file URI.
     * All actions run within a safe Room transaction.
     */
    suspend fun importDatabaseFromUri(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(StandardCharsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("امکان خواندن فایل پشتیبان وجود ندارد."))

            val root = JSONObject(content)
            if (!root.has("appName") || root.getString("appName") != "AutoCare") {
                return@withContext Result.failure(Exception("فایل انتخاب‌شده یک فایل پشتیبان معتبر اتوکِر (AutoCare) نیست."))
            }

            val vehicles = mutableListOf<Vehicle>()
            val vArray = root.optJSONArray("vehicles") ?: JSONArray()
            for (i in 0 until vArray.length()) {
                val o = vArray.getJSONObject(i)
                vehicles.add(
                    Vehicle(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        currentKm = o.getLong("currentKm"),
                        year = if (o.isNull("year")) null else o.getInt("year"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val items = mutableListOf<MaintenanceItem>()
            val iArray = root.optJSONArray("items") ?: JSONArray()
            for (i in 0 until iArray.length()) {
                val o = iArray.getJSONObject(i)
                items.add(
                    MaintenanceItem(
                        id = o.getLong("id"),
                        vehicleId = o.getLong("vehicleId"),
                        name = o.getString("name"),
                        category = o.getString("category"),
                        intervalKm = if (o.isNull("intervalKm")) null else o.getLong("intervalKm"),
                        intervalMonths = if (o.isNull("intervalMonths")) null else o.getInt("intervalMonths"),
                        description = o.optString("description", ""),
                        isActive = o.optBoolean("isActive", true),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val records = mutableListOf<MaintenanceRecord>()
            val rArray = root.optJSONArray("records") ?: JSONArray()
            for (i in 0 until rArray.length()) {
                val o = rArray.getJSONObject(i)
                records.add(
                    MaintenanceRecord(
                        id = o.getLong("id"),
                        itemId = o.getLong("itemId"),
                        date = if (o.isNull("date")) null else o.getLong("date"),
                        km = o.getLong("km"),
                        cost = if (o.isNull("cost")) null else o.getLong("cost"),
                        description = o.optString("description", ""),
                        parts = o.optString("parts", ""),
                        workshop = o.optString("workshop", ""),
                        invoiceUri = if (o.isNull("invoiceUri")) null else o.getString("invoiceUri"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val plans = mutableListOf<PlannedService>()
            val pArray = root.optJSONArray("plans") ?: JSONArray()
            for (i in 0 until pArray.length()) {
                val o = pArray.getJSONObject(i)
                val statusStr = o.optString("status", "PENDING")
                val status = try {
                    PlanStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    PlanStatus.PENDING
                }
                plans.add(
                    PlannedService(
                        id = o.getLong("id"),
                        itemId = o.getLong("itemId"),
                        vehicleId = o.getLong("vehicleId"),
                        targetKm = if (o.isNull("targetKm")) null else o.getLong("targetKm"),
                        targetDate = if (o.isNull("targetDate")) null else o.getLong("targetDate"),
                        notes = o.optString("notes", ""),
                        status = status,
                        convertedToRecordId = if (o.isNull("convertedToRecordId")) null else o.getLong("convertedToRecordId"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val reminders = mutableListOf<ReminderSetting>()
            val remArray = root.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until remArray.length()) {
                val o = remArray.getJSONObject(i)
                reminders.add(
                    ReminderSetting(
                        id = o.getLong("id"),
                        plannedServiceId = o.getLong("plannedServiceId"),
                        daysBefore = o.getInt("daysBefore"),
                        isEnabled = o.optBoolean("isEnabled", true),
                        notificationId = o.optInt("notificationId", 0),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Execute in an atomic transaction
            db.withTransaction {
                reminderDao.deleteAllReminders()
                plannedDao.deleteAllServices()
                recordDao.deleteAllRecords()
                itemDao.deleteAllItems()
                vehicleDao.deleteAllVehicles()

                vehicleDao.insertVehicles(vehicles)
                itemDao.insertItems(items)
                recordDao.insertRecords(records)
                plannedDao.insertServices(plans)
                reminderDao.insertReminders(reminders)
            }

            // Reschedule notification alarms for all restored pending plans
            val workRequest = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)

            Result.success(
                BackupStats(
                    vehicleCount = vehicles.size,
                    itemCount = items.size,
                    recordCount = records.size,
                    planCount = plans.size,
                    reminderCount = reminders.size
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("خطا در پردازش و بازیابی فایل پشتیبان: ${e.message}"))
        }
    }
}
