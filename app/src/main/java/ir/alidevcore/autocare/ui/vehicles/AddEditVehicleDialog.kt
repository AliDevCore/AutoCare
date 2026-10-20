package ir.alidevcore.autocare.ui.vehicles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.alidevcore.autocare.data.model.Vehicle
import ir.alidevcore.autocare.util.NumberUtil

@Composable
fun AddEditVehicleDialog(
    vehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, currentKm: Long, year: Int?) -> Unit
) {
    var name by remember { mutableStateOf(vehicle?.name ?: "") }
    var kmText by remember { mutableStateOf(vehicle?.currentKm?.toString() ?: "") }
    var yearText by remember { mutableStateOf(vehicle?.year?.toString() ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showKmDecreaseWarning by remember { mutableStateOf(false) }

    if (showKmDecreaseWarning) {
        AlertDialog(
            onDismissRequest = { showKmDecreaseWarning = false },
            title = { Text("هشدار کاهش کیلومتر") },
            text = {
                Text("کیلومتر واردشده از آخرین مقدار ثبت‌شده کمتر است. این تغییر ممکن است ناشی از تعویض یا اصلاح کیلومترشمار باشد. آیا مطمئن هستید؟")
            },
            confirmButton = {
                Button(onClick = {
                    showKmDecreaseWarning = false
                    val km = NumberUtil.parsePersianNumber(kmText) ?: 0L
                    val yr = yearText.trim().toIntOrNull()
                    onSave(name.trim(), km, yr)
                }) {
                    Text("بله، ثبت شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKmDecreaseWarning = false }) {
                    Text("بازگشت و اصلاح")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vehicle == null) "افزودن خودرو جدید" else "ویرایش مشخصات خودرو") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام خودرو (مثلاً: پژو ۲۰۶، سمند)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null && name.isBlank()
                )

                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it },
                    label = { Text("کیلومتر فعلی خودرو*") },
                    placeholder = { Text("مثلاً: ۱۲۵۰۰۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it },
                    label = { Text("سال ساخت (اختیاری)") },
                    placeholder = { Text("مثلاً: ۱۳۹۸ یا 2020") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cleanName = name.trim()
                if (cleanName.isBlank()) {
                    errorText = "نام خودرو الزامی است"
                    return@Button
                }
                val km = NumberUtil.parsePersianNumber(kmText)
                if (km == null || km < 0) {
                    errorText = "کیلومتر فعلی معتبر نیست"
                    return@Button
                }
                val yr = if (yearText.isNotBlank()) NumberUtil.parsePersianNumber(yearText)?.toInt() else null

                // Check for km decrease warning when editing
                if (vehicle != null && km < vehicle.currentKm) {
                    showKmDecreaseWarning = true
                    return@Button
                }

                onSave(cleanName, km, yr)
            }) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
