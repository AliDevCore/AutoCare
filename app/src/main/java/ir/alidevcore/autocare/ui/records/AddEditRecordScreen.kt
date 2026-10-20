package ir.alidevcore.autocare.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.ui.components.*
import ir.alidevcore.autocare.util.JalaliDateUtil
import ir.alidevcore.autocare.util.NumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecordScreen(
    viewModel: RecordViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBulkRecord: ((Long?) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showNextDatePicker by remember { mutableStateOf(false) }
    var itemMenuExpanded by remember { mutableStateOf(false) }
    var showKmUpdatePrompt by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = uiState.date,
            onDateSelected = { timestamp ->
                viewModel.onDateChanged(timestamp)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showNextDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = uiState.nextDate ?: System.currentTimeMillis(),
            onDateSelected = { timestamp ->
                viewModel.onNextDateChanged(timestamp)
                showNextDatePicker = false
            },
            onDismiss = { showNextDatePicker = false }
        )
    }

    // Prompt to update vehicle's current km if record km > vehicle current km
    if (showKmUpdatePrompt) {
        val enteredKm = uiState.kmText.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { showKmUpdatePrompt = false },
            title = { Text("به‌روزرسانی کیلومتر فعلی خودرو") },
            text = {
                Text(
                    "کیلومتر ثبت‌شده (${NumberUtil.formatKm(enteredKm)}) از کیلومتر فعلی خودرو (${NumberUtil.formatKm(uiState.vehicle?.currentKm ?: 0L)}) بیشتر است.\nآیا مایلید کیلومتر فعلی خودرو نیز به ${NumberUtil.formatKm(enteredKm)} به‌روزرسانی شود؟"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showKmUpdatePrompt = false
                    viewModel.saveRecord(updateVehicleKm = true)
                }) {
                    Text("بله، به‌روزرسانی شود")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showKmUpdatePrompt = false
                    viewModel.saveRecord(updateVehicleKm = false)
                }) {
                    Text("خیر، فقط سابقه ثبت شود")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = if (uiState.isEditing) "ویرایش سابقه سرویس" else "ثبت سرویس انجام‌شده",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notice if converting from plan
                if (uiState.planId != null && uiState.planId!! > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "این سرویس بر اساس یک برنامه آینده در حال ثبت است. پس از ذخیره، برنامه به عنوان «انجام‌شده» علامت‌گذاری می‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // 1. Service Item Selection
                Text(
                    text = "مشخصات سرویس",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!uiState.isEditing && onNavigateToBulkRecord != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBulkRecord(uiState.selectedItem?.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ثبت دسته‌جمعی سوابق گذشته",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "برای ثبت سریع چندین نوبت سرویس در گذشته (مثلاً در چند کیلومتر مختلف بدون تاریخ)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.selectedItem?.let { "${it.name} (${it.category})" } ?: "انتخاب سرویس...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("سرویس یا قطعه*") },
                        trailingIcon = {
                            IconButton(onClick = { itemMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { itemMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = itemMenuExpanded,
                        onDismissRequest = { itemMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        uiState.items.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold)
                                        Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    viewModel.onItemSelected(item)
                                    itemMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Date Selection (Shamsi)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onToggleDateUnknown(!uiState.isDateUnknown) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isDateUnknown,
                            onCheckedChange = { viewModel.onToggleDateUnknown(it) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "تاریخ دقیق را به یاد ندارم (ثبت سابقه فقط با کیلومتر)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!uiState.isDateUnknown) {
                        OutlinedTextField(
                            value = JalaliDateUtil.timestampToJalaliStringWithMonthName(uiState.date),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاریخ انجام سرویس (شمسی)*") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "انتخاب تاریخ")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "این سابقه بدون تاریخ مشخص و صرفاً با ثبت کارکرد کیلومتر ذخیره می‌شود (مناسب سوابق گذشته خودرو).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. Kilometer
                OutlinedTextField(
                    value = uiState.kmText,
                    onValueChange = { viewModel.onKmChanged(it) },
                    label = { Text("کیلومتر هنگام سرویس*") },
                    placeholder = { Text("مثلاً: ۱۲۵۰۰۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Cost (Tomans) - Optional
                OutlinedTextField(
                    value = uiState.costText,
                    onValueChange = { viewModel.onCostChanged(it) },
                    label = { Text("هزینه (تومان) - اختیاری") },
                    placeholder = { Text("مثلاً: ۱۵۰۰۰۰۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        val parsed = uiState.costText.toLongOrNull()
                        if (parsed != null && parsed > 0) {
                            Text("${NumberUtil.formatCurrency(parsed)} تومان")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Supplementary info (Optional)
                OutlinedTextField(
                    value = uiState.parts,
                    onValueChange = { viewModel.onPartsChanged(it) },
                    label = { Text("مشخصات قطعه یا برند مصرفی (اختیاری)") },
                    placeholder = { Text("مثلاً: بهران سوپر رانا 5W30") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.workshop,
                    onValueChange = { viewModel.onWorkshopChanged(it) },
                    label = { Text("نام تعمیرگاه یا مکانیک (اختیاری)") },
                    placeholder = { Text("مثلاً: اتوسرویس پارس") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = { Text("توضیحات و یادداشت (اختیاری)") },
                    placeholder = { Text("توضیحات تکمیلی...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // 6. Next Schedule Section (Only when creating a new record)
                if (!uiState.isEditing) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "تعیین نوبت بعدی سرویس",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NextScheduleChip(
                            label = "بدون نوبت",
                            selected = uiState.nextScheduleMode == NextScheduleMode.NONE,
                            onClick = { viewModel.onNextScheduleModeChanged(NextScheduleMode.NONE) }
                        )
                        NextScheduleChip(
                            label = "کیلومتر",
                            selected = uiState.nextScheduleMode == NextScheduleMode.ONLY_KM,
                            onClick = { viewModel.onNextScheduleModeChanged(NextScheduleMode.ONLY_KM) }
                        )
                        NextScheduleChip(
                            label = "تاریخ",
                            selected = uiState.nextScheduleMode == NextScheduleMode.ONLY_DATE,
                            onClick = { viewModel.onNextScheduleModeChanged(NextScheduleMode.ONLY_DATE) }
                        )
                        NextScheduleChip(
                            label = "هردو",
                            selected = uiState.nextScheduleMode == NextScheduleMode.BOTH,
                            onClick = { viewModel.onNextScheduleModeChanged(NextScheduleMode.BOTH) }
                        )
                    }

                    if (uiState.nextScheduleMode in listOf(NextScheduleMode.ONLY_KM, NextScheduleMode.BOTH)) {
                        OutlinedTextField(
                            value = uiState.nextKmText,
                            onValueChange = { viewModel.onNextKmChanged(it) },
                            label = { Text("کیلومتر نوبت بعدی") },
                            placeholder = { Text("مثلاً: ۱۳۰۰۰۰") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (uiState.nextScheduleMode in listOf(NextScheduleMode.ONLY_DATE, NextScheduleMode.BOTH)) {
                        OutlinedTextField(
                            value = uiState.nextDate?.let { JalaliDateUtil.timestampToJalaliStringWithMonthName(it) } ?: "انتخاب تاریخ نوبت بعدی...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاریخ نوبت بعدی") },
                            trailingIcon = {
                                IconButton(onClick = { showNextDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNextDatePicker = true }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Save button
                Button(
                    onClick = {
                        val enteredKm = uiState.kmText.toLongOrNull()
                        val currentVehicleKm = uiState.vehicle?.currentKm ?: 0L

                        if (enteredKm != null && enteredKm > currentVehicleKm) {
                            showKmUpdatePrompt = true
                        } else {
                            viewModel.saveRecord(updateVehicleKm = false)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isEditing) "ذخیره تغییرات" else "ثبت نهایی سابقه", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun RowScope.NextScheduleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.weight(1f)
    )
}
