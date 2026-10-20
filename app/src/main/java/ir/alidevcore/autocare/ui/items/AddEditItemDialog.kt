package ir.alidevcore.autocare.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.util.NumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    item: MaintenanceItem? = null,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, intervalKm: Long?, intervalMonths: Int?, description: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: (availableCategories.firstOrNull() ?: "سرویس‌های عمومی")) }
    var isCustomCategory by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }
    var intervalKmText by remember { mutableStateOf(item?.intervalKm?.toString() ?: "") }
    var intervalMonthsText by remember { mutableStateOf(item?.intervalMonths?.toString() ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var isActive by remember { mutableStateOf(item?.isActive ?: true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "افزودن قلم تعمیراتی / سرویس" else "ویرایش قلم سرویس") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام سرویس یا قطعه (مانند: روغن موتور)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                if (!isCustomCategory) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("دسته‌بندی") },
                            trailingIcon = {
                                IconButton(onClick = { categoryMenuExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ ایجاد دسته‌بندی جدید...") },
                                onClick = {
                                    categoryMenuExpanded = false
                                    isCustomCategory = true
                                }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = { customCategoryText = it },
                        label = { Text("نام دسته‌بندی جدید*") },
                        trailingIcon = {
                            TextButton(onClick = { isCustomCategory = false }) {
                                Text("انتخاب از لیست")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = intervalKmText,
                    onValueChange = { intervalKmText = it },
                    label = { Text("فاصله دوره‌ای کیلومتر (اختیاری)") },
                    placeholder = { Text("مثلاً: ۵۰۰۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = intervalMonthsText,
                    onValueChange = { intervalMonthsText = it },
                    label = { Text("فاصله دوره‌ای ماه (اختیاری)") },
                    placeholder = { Text("مثلاً: ۶") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    placeholder = { Text("مثلاً: نوع روغن 10W40 SN، فیلتر سرکان") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (item != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("وضعیت این قلم فعال باشد")
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                }

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
                    errorText = "نام سرویس یا قطعه الزامی است"
                    return@Button
                }

                val finalCategory = if (isCustomCategory) {
                    val c = customCategoryText.trim()
                    if (c.isBlank()) {
                        errorText = "نام دسته‌بندی جدید الزامی است"
                        return@Button
                    }
                    c
                } else {
                    category
                }

                val km = if (intervalKmText.isNotBlank()) NumberUtil.parsePersianNumber(intervalKmText) else null
                val months = if (intervalMonthsText.isNotBlank()) NumberUtil.parsePersianNumber(intervalMonthsText)?.toInt() else null

                onSave(cleanName, finalCategory, km, months, description.trim(), isActive)
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
