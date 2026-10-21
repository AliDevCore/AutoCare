package ir.alidevcore.autocare.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alidevcore.autocare.data.model.MaintenanceRecord
import ir.alidevcore.autocare.ui.components.*
import ir.alidevcore.autocare.util.JalaliDateUtil
import ir.alidevcore.autocare.util.NumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsHistoryScreen(
    viewModel: RecordsHistoryViewModel,
    onNavigateBack: () -> Unit,
    onEditRecord: (recordId: Long) -> Unit,
    onAddNewRecord: () -> Unit,
    onBulkRecord: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var recordToDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showKmFilterDialog by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showFromDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = uiState.fromDate ?: System.currentTimeMillis(),
            onDateSelected = { timestamp ->
                viewModel.setDateRange(timestamp, uiState.toDate)
                showFromDatePicker = false
            },
            onDismiss = { showFromDatePicker = false }
        )
    }

    if (showToDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = uiState.toDate ?: System.currentTimeMillis(),
            onDateSelected = { timestamp ->
                viewModel.setDateRange(uiState.fromDate, timestamp)
                showToDatePicker = false
            },
            onDismiss = { showToDatePicker = false }
        )
    }

    if (showKmFilterDialog) {
        var fromKmText by remember { mutableStateOf(uiState.fromKm?.toString() ?: "") }
        var toKmText by remember { mutableStateOf(uiState.toKm?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showKmFilterDialog = false },
            title = { Text("فیلتر بر اساس بازه کیلومتر") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fromKmText,
                        onValueChange = { fromKmText = it },
                        label = { Text("از کیلومتر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = toKmText,
                        onValueChange = { toKmText = it },
                        label = { Text("تا کیلومتر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val f = fromKmText.trim().let { if (it.isNotBlank()) it.toLongOrNull() else null }
                    val t = toKmText.trim().let { if (it.isNotBlank()) it.toLongOrNull() else null }
                    viewModel.setKmRange(f, t)
                    showKmFilterDialog = false
                }) {
                    Text("اعمال فیلتر")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setKmRange(null, null)
                    showKmFilterDialog = false
                }) {
                    Text("حذف فیلتر")
                }
            }
        )
    }

    recordToDelete?.let { record ->
        ConfirmDeleteDialog(
            title = "حذف سابقه سرویس",
            message = "آیا از حذف این سابقه اطمینان دارید؟ این عملیات غیرقابل بازگشت است و تاثیری بر سایر سوابق نخواهد داشت.",
            onConfirm = {
                viewModel.deleteRecord(record)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = uiState.filterItemName?.let { "سوابق «$it»" } ?: "تاریخچه و سوابق سرویس‌ها",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onBulkRecord) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "ثبت دسته‌جمعی سوابق")
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "مرتب‌سازی")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("جدیدترین تاریخ") },
                                onClick = {
                                    viewModel.onSortOptionChanged(SortOption.DATE_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("قدیمی‌ترین تاریخ") },
                                onClick = {
                                    viewModel.onSortOptionChanged(SortOption.DATE_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("بیشترین کیلومتر") },
                                onClick = {
                                    viewModel.onSortOptionChanged(SortOption.KM_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("کمترین کیلومتر") },
                                onClick = {
                                    viewModel.onSortOptionChanged(SortOption.KM_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNewRecord,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ثبت سابقه جدید") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("جستوجوی قطعه، تعمیرگاه، قطعات مصرفی یا یادداشت...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("همه دسته‌ها") }
                    )
                }
                items(uiState.categories) { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = {
                            viewModel.onCategorySelected(if (uiState.selectedCategory == cat) null else cat)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            // Advanced Filter triggers row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Km Filter button
                OutlinedButton(
                    onClick = { showKmFilterDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = if (uiState.fromKm != null || uiState.toKm != null) {
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("بازه کیلومتر", fontSize = 12.sp)
                }

                // Date Filter button
                OutlinedButton(
                    onClick = { showFromDatePicker = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = if (uiState.fromDate != null || uiState.toDate != null) {
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (uiState.fromDate != null) "از ${JalaliDateUtil.formatShortDate(uiState.fromDate!!)}" else "بازه تاریخ",
                        fontSize = 12.sp
                    )
                }

                if (uiState.fromDate != null || uiState.toDate != null || uiState.fromKm != null || uiState.toKm != null || uiState.selectedCategory != null) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("حذف همه فیلترها", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Results count
            Text(
                text = "${NumberUtil.toPersianDigits(uiState.filteredRecords.size)} سابقه ثبت‌شده",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.filteredRecords.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "هیچ سابقه‌ای یافت نشد",
                        description = "سابقه‌ای با این فیلترها و مشخصات در سیستم وجود ندارد.",
                        actionText = "ثبت سابقه جدید",
                        onAction = onAddNewRecord
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredRecords, key = { it.record.id }) { item ->
                            RecordDetailCard(
                                recordInfo = item,
                                onEdit = { onEditRecord(item.record.id) },
                                onDelete = { recordToDelete = item.record }
                            )
                        }
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordDetailCard(
    recordInfo: RecordWithItemInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Item name, date, options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recordInfo.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = recordInfo.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recordInfo.record.cost != null) {
                        CostChip(amount = recordInfo.record.cost)
                        Spacer(Modifier.width(4.dp))
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "عملیات")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("ویرایش") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Mileage and Date details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = NumberUtil.formatKm(recordInfo.record.km),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    val dateText = recordInfo.record.date?.let {
                        JalaliDateUtil.timestampToJalaliStringWithMonthName(it)
                    } ?: "تاریخ نامشخص (فقط کارکرد)"
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Workshop
            if (recordInfo.record.workshop.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "تعمیرگاه: ${recordInfo.record.workshop}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Parts / materials
            if (recordInfo.record.parts.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "قطعات: ${recordInfo.record.parts}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Description
            if (recordInfo.record.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "توضیحات: ${recordInfo.record.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
