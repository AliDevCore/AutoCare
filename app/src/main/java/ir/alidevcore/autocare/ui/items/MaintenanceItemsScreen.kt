package ir.alidevcore.autocare.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.ui.components.*
import ir.alidevcore.autocare.util.NumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceItemsScreen(
    viewModel: ItemViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecordService: (itemId: Long) -> Unit,
    onNavigateToItemHistory: (itemId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MaintenanceItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MaintenanceItem?>(null) }

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

    if (showAddDialog) {
        AddEditItemDialog(
            availableCategories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onSave = { name, category, km, months, desc, _ ->
                viewModel.addItem(name, category, km, months, desc)
                showAddDialog = false
            }
        )
    }

    itemToEdit?.let { item ->
        AddEditItemDialog(
            item = item,
            availableCategories = uiState.categories,
            onDismiss = { itemToEdit = null },
            onSave = { name, category, km, months, desc, active ->
                viewModel.updateItem(
                    item.copy(
                        name = name,
                        category = category,
                        intervalKm = km,
                        intervalMonths = months,
                        description = desc,
                        isActive = active
                    )
                )
                itemToEdit = null
            }
        )
    }

    itemToDelete?.let { item ->
        ConfirmDeleteDialog(
            title = "حذف قلم «${item.name}»",
            message = "آیا از حذف این قلم اطمینان دارید؟ تمام سوابق و برنامه‌های مرتبط با این قلم نیز حذف خواهند شد.",
            onConfirm = {
                viewModel.deleteItem(item)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "اقلام و سرویس‌های دوره‌ای",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("قلم جدید") },
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
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("جستوجوی نام قطعه، دسته‌بندی یا توضیحات...") },
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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

            Spacer(Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.filteredItems.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Build,
                        title = "هیچ قلمی یافت نشد",
                        description = "برای این خودرو هنوز قلم تعمیراتی با این مشخصات ثبت نشده است.",
                        actionText = "افزودن قلم جدید",
                        onAction = { showAddDialog = true }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredItems, key = { it.id }) { item ->
                            ItemRowCard(
                                item = item,
                                onRecord = { onNavigateToRecordService(item.id) },
                                onHistory = { onNavigateToItemHistory(item.id) },
                                onEdit = { itemToEdit = item },
                                onDelete = { itemToDelete = item }
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
fun ItemRowCard(
    item: MaintenanceItem,
    onRecord: () -> Unit,
    onHistory: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!item.isActive) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "غیرفعال",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                            text = { Text("ثبت انجام این سرویس") },
                            leadingIcon = { Icon(Icons.Default.AddTask, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRecord()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("مشاهده سوابق این قلم") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onHistory()
                            }
                        )
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

            Spacer(Modifier.height(8.dp))

            // Intervals info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (item.intervalKm != null) {
                    Text(
                        text = "دوره: ${NumberUtil.formatKm(item.intervalKm)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.intervalMonths != null) {
                    Text(
                        text = "دوره: ${NumberUtil.toPersianDigits(item.intervalMonths)} ماهه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.intervalKm == null && item.intervalMonths == null) {
                    Text(
                        text = "بدون دوره پیشنهادی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
