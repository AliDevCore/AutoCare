package ir.alidevcore.autocare.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alidevcore.autocare.ui.components.*
import ir.alidevcore.autocare.util.JalaliDateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlanScreen(
    viewModel: PlanViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var itemMenuExpanded by remember { mutableStateOf(false) }

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
            initialTimestamp = uiState.targetDate ?: System.currentTimeMillis(),
            onDateSelected = { timestamp ->
                viewModel.onTargetDateChanged(timestamp)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = if (uiState.isEditing) "ویرایش برنامه سرویس آینده" else "برنامه‌ریزی سرویس آینده",
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
                Text(
                    text = "اطلاعات برنامه سرویس",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Item selector
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

                // Target Km
                OutlinedTextField(
                    value = uiState.targetKmText,
                    onValueChange = { viewModel.onTargetKmChanged(it) },
                    label = { Text("کیلومتر هدف (اختیاری)") },
                    placeholder = { Text("مثلاً: ۱۳۵۰۰۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Date (Shamsi)
                OutlinedTextField(
                    value = uiState.targetDate?.let { JalaliDateUtil.timestampToJalaliStringWithMonthName(it) } ?: "انتخاب تاریخ هدف (شمسی)...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تاریخ هدف (اختیاری)") },
                    trailingIcon = {
                        Row {
                            if (uiState.targetDate != null) {
                                IconButton(onClick = { viewModel.onTargetDateChanged(null) }) {
                                    Icon(Icons.Default.Clear, contentDescription = "حذف تاریخ")
                                }
                            }
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                // Reminder checkboxes (only if targetDate is selected)
                if (uiState.targetDate != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "زمان‌های یادآوری اعلان محلی",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            ReminderOptionCheckbox(
                                label = "در روز موعد سرویس (ساعت ۹ صبح)",
                                checked = uiState.selectedReminderDays.contains(0),
                                onToggle = { viewModel.toggleReminderDay(0) }
                            )
                            ReminderOptionCheckbox(
                                label = "۱ روز قبل از موعد",
                                checked = uiState.selectedReminderDays.contains(1),
                                onToggle = { viewModel.toggleReminderDay(1) }
                            )
                            ReminderOptionCheckbox(
                                label = "۳ روز قبل از موعد",
                                checked = uiState.selectedReminderDays.contains(3),
                                onToggle = { viewModel.toggleReminderDay(3) }
                            )
                            ReminderOptionCheckbox(
                                label = "۱ هفته قبل از موعد (۷ روز قبل)",
                                checked = uiState.selectedReminderDays.contains(7),
                                onToggle = { viewModel.toggleReminderDay(7) }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("یادداشت و نکات (اختیاری)") },
                    placeholder = { Text("نکات لازم قبل از رفتن به تعمیرگاه...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.savePlan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isEditing) "ذخیره تغییرات" else "ثبت برنامه سرویس", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReminderOptionCheckbox(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
