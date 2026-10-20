package ir.alidevcore.autocare.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.ui.components.JalaliDatePickerDialog
import ir.alidevcore.autocare.ui.components.LoadingState
import ir.alidevcore.autocare.util.JalaliDateUtil
import ir.alidevcore.autocare.util.NumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkRecordScreen(
    viewModel: BulkRecordViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var itemMenuExpanded by remember { mutableStateOf(false) }
    var activeDatePickerRowId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Date Picker Dialog
    activeDatePickerRowId?.let { rowId ->
        val currentRow = uiState.rows.find { it.id == rowId }
        val initialTs = currentRow?.date ?: System.currentTimeMillis()
        JalaliDatePickerDialog(
            initialTimestamp = initialTs,
            onDateSelected = { timestamp ->
                viewModel.updateRowDate(rowId, timestamp)
                activeDatePickerRowId = null
            },
            onDismiss = { activeDatePickerRowId = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت دسته‌جمعی سوابق گذشته") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (uiState.rows.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllRows() }) {
                            Icon(Icons.Default.ClearAll, contentDescription = "پاک کردن همه")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoading) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveAllRecords() },
                            enabled = !uiState.isSaving && uiState.rows.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("در حال ذخیره‌سازی...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                val countText = if (uiState.rows.isNotEmpty()) " (${NumberUtil.toPersianDigits(uiState.rows.size)})" else ""
                                Text("ثبت همه سوابق$countText", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    // 1. Informational Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "برای سوابق چند سال گذشته خودرو، نیازی به دانستن تاریخ دقیق نیست؛ سوابق بر اساس کیلومتر ذخیره می‌شوند و نوبت بعدی به صورت دقیق محاسبه خواهد شد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // 2. Item Selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "انتخاب قطعه یا سرویس",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))

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

                            uiState.selectedItem?.let { item ->
                                Spacer(Modifier.height(8.dp))
                                val intervalText = when {
                                    item.intervalKm != null && item.intervalMonths != null ->
                                        "دوره استاندارد: هر ${NumberUtil.formatKm(item.intervalKm)} یا ${NumberUtil.toPersianDigits(item.intervalMonths)} ماه"
                                    item.intervalKm != null ->
                                        "دوره استاندارد: هر ${NumberUtil.formatKm(item.intervalKm)}"
                                    else -> "بدون دوره تعریف‌شده"
                                }
                                Text(
                                    text = intervalText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 3. Quick Input Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ورود سریع با لیست کارکردها",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "کیلومترها را با کاما، فاصله یا خط جدید وارد کنید (مثال: ۳۰۰۰۰، ۶۰۰۰۰، ۹۰۰۰۰، ۱۳۰۰۰۰ یا 30k 60k 90k 130k):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.quickKmInput,
                                onValueChange = { viewModel.onQuickKmInputChanged(it) },
                                placeholder = { Text("30000, 60000, 90000, 130000") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { viewModel.parseAndAddFromQuickInput() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("تبدیل به ردیف‌های سابقه")
                            }
                        }
                    }
                }

                // 4. Header for rows
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ردیف‌های سوابق (${NumberUtil.toPersianDigits(uiState.rows.size)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { viewModel.addNewEmptyRow() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("افزودن ردیف", fontSize = 12.sp)
                        }
                    }
                }

                // 5. Empty State or Rows List
                if (uiState.rows.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.PostAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "هنوز ردیفی اضافه نشده است",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "از کادر ورود سریع بالا یا دکمه «افزودن ردیف» استفاده کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(uiState.rows, key = { _, row -> row.id }) { index, row ->
                        BulkRowCard(
                            index = index,
                            row = row,
                            onKmChanged = { viewModel.updateRowKm(row.id, it) },
                            onOpenDatePicker = { activeDatePickerRowId = row.id },
                            onClearDate = { viewModel.updateRowDate(row.id, null) },
                            onCostChanged = { viewModel.updateRowCost(row.id, it) },
                            onDescriptionChanged = { viewModel.updateRowDescription(row.id, it) },
                            onDelete = { viewModel.removeRow(row.id) }
                        )
                    }
                }

                // 6. Post-Save Options
                if (uiState.rows.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "تنظیمات پس از ذخیره",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))

                                // Update vehicle km
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.toggleUpdateVehicleKm(!uiState.updateVehicleKm) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = uiState.updateVehicleKm,
                                        onCheckedChange = { viewModel.toggleUpdateVehicleKm(it) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "به‌روزرسانی کارکرد فعلی خودرو به آخرین کیلومتر واردشده (در صورت بیشتر بودن)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // Create next planned service
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.toggleCreateNextPlan(!uiState.createNextPlan) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = uiState.createNextPlan,
                                        onCheckedChange = { viewModel.toggleCreateNextPlan(it) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "برنامه‌ریزی خودکار نوبت بعدی سرویس بر اساس آخرین کیلومتر واردشده و دوره استاندارد",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun BulkRowCard(
    index: Int,
    row: BulkRowItem,
    onKmChanged: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    onClearDate: () -> Unit,
    onCostChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(row.costText.isNotBlank() || row.description.isNotBlank() || row.date != null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "نوبت ${NumberUtil.toPersianDigits(index + 1)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { expandedDetails = !expandedDetails }) {
                        Text(if (expandedDetails) "جزئیات کمتر" else "افزودن تاریخ/هزینه/یادداشت", fontSize = 11.sp)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف ردیف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Kilometer (Mandatory)
            OutlinedTextField(
                value = row.kmText,
                onValueChange = onKmChanged,
                label = { Text("کیلومتر کارکرد*") },
                placeholder = { Text("مثلاً: ۳۰۰۰۰") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = expandedDetails) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date (Optional)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = row.date?.let { JalaliDateUtil.timestampToJalaliString(it) } ?: "بدون تاریخ مشخص",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاریخ انجام (شمسی - اختیاری)") },
                            trailingIcon = {
                                if (row.date != null) {
                                    IconButton(onClick = onClearDate) {
                                        Icon(Icons.Default.Close, contentDescription = "حذف تاریخ")
                                    }
                                } else {
                                    IconButton(onClick = onOpenDatePicker) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "انتخاب تاریخ")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenDatePicker() }
                        )
                    }

                    // Cost (Optional)
                    OutlinedTextField(
                        value = row.costText,
                        onValueChange = onCostChanged,
                        label = { Text("هزینه (تومان - اختیاری)") },
                        placeholder = { Text("مثلاً: ۵۰۰۰۰۰") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description (Optional)
                    OutlinedTextField(
                        value = row.description,
                        onValueChange = onDescriptionChanged,
                        label = { Text("توضیحات یا مارک قطعه (اختیاری)") },
                        placeholder = { Text("مثلاً: شمع سوزنی NGK") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
