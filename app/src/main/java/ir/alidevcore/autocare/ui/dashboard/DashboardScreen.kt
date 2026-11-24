package ir.alidevcore.autocare.ui.dashboard

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alidevcore.autocare.data.model.MaintenanceItem
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.relations.ItemWithLastRecord
import ir.alidevcore.autocare.data.model.relations.ServiceStatus
import ir.alidevcore.autocare.ui.components.*
import ir.alidevcore.autocare.ui.items.AddEditItemDialog
import ir.alidevcore.autocare.ui.items.ItemRowCard
import ir.alidevcore.autocare.ui.theme.*
import ir.alidevcore.autocare.util.JalaliDateUtil
import ir.alidevcore.autocare.util.NumberUtil
import kotlinx.coroutines.launch

enum class DashboardTab(val title: String, val icon: ImageVector) {
    STATUS("داشبورد", Icons.Default.DirectionsCar),
    PLANS("برنامه‌ها", Icons.Default.Event),
    HISTORY("سوابق", Icons.Default.History),
    ITEMS("اقلام", Icons.Default.Build),
    EXPENSES("هزینه‌ها", Icons.Default.MonetizationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddRecord: (itemId: Long?, planId: Long?) -> Unit,
    onNavigateToEditRecord: (recordId: Long) -> Unit = {},
    onNavigateToAddPlan: (itemId: Long?, planId: Long?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToBulkRecord: (itemId: Long?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    var historyPage by remember { mutableStateOf(1) }
    var recordToDelete by remember { mutableStateOf<RecordDisplay?>(null) }
    var showKmDialog by remember { mutableStateOf(false) }
    var showKmDecreaseWarning by remember { mutableStateOf<Long?>(null) }
    var kmInput by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<PlannedServiceDisplay?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MaintenanceItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MaintenanceItem?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    BackHandler(enabled = selectedTab != DashboardTab.STATUS) {
        viewModel.setSelectedTab(DashboardTab.STATUS)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDashboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importDatabase(
                uri = uri,
                onSuccess = { stats ->
                    scope.launch {
                        snackbarHostState.showSnackbar("دیتابیس با موفقیت بازیابی شد (${NumberUtil.toPersianDigits(stats.vehicleCount)} خودرو، ${NumberUtil.toPersianDigits(stats.recordCount)} سابقه)")
                    }
                },
                onError = { err ->
                    scope.launch { snackbarHostState.showSnackbar(err) }
                }
            )
        }
    }

    LaunchedEffect(uiState.showKmUpdateSuccess) {
        if (uiState.showKmUpdateSuccess) {
            snackbarHostState.showSnackbar("کیلومتر خودرو به‌روزرسانی شد")
            viewModel.clearKmUpdateSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showKmDecreaseWarning != null) {
        AlertDialog(
            onDismissRequest = { showKmDecreaseWarning = null },
            title = { Text("هشدار کاهش کیلومتر", fontWeight = FontWeight.Bold) },
            text = {
                Text("کیلومتر واردشده (${NumberUtil.formatKm(showKmDecreaseWarning!!)}) از کیلومتر فعلی کمتر است. آیا از تغییر کیلومترشمار مطمئن هستید؟")
            },
            confirmButton = {
                Button(onClick = {
                    val km = showKmDecreaseWarning!!
                    showKmDecreaseWarning = null
                    showKmDialog = false
                    viewModel.updateCurrentKm(km)
                }) {
                    Text("بله، تأیید تغییر")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKmDecreaseWarning = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showKmDialog) {
        val currentKm = uiState.vehicle?.currentKm ?: 0L
        AlertDialog(
            onDismissRequest = { showKmDialog = false },
            title = { Text("به‌روزرسانی کیلومتر فعلی خودرو", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "کیلومتر فعلی ثبت‌شده: ${NumberUtil.formatKm(currentKm)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = kmInput,
                        onValueChange = { kmInput = NumberUtil.formatKmInput(it) },
                        label = { Text("کیلومتر جدید") },
                        placeholder = { Text("مثال: 120,000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newKm = NumberUtil.parsePersianNumber(kmInput)
                        if (newKm != null && newKm >= 0) {
                            if (newKm < currentKm) {
                                showKmDecreaseWarning = newKm
                            } else {
                                showKmDialog = false
                                viewModel.updateCurrentKm(newKm)
                            }
                        }
                    }
                ) {
                    Text("ثبت و ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("بازیابی کل دیتابیس", fontWeight = FontWeight.Bold) },
            text = {
                Text("توجه: با بازیابی دیتابیس، تمامی خودروها و سوابق فعلی با اطلاعات فایل پشتیبان جایگزین خواهند شد. آیا مطمئن هستید؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmDialog = false
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("انتخاب فایل و بازیابی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (planToDelete != null) {
        ConfirmDeleteDialog(
            title = "حذف برنامه سرویس",
            message = "آیا از حذف برنامه سرویس «${planToDelete?.itemName}» اطمینان دارید؟ یادآورهای ثبت‌شده نیز لغو خواهند شد.",
            onConfirm = {
                val p = planToDelete!!
                planToDelete = null
                viewModel.deletePlan(p.plan)
                scope.launch {
                    snackbarHostState.showSnackbar("برنامه با موفقیت حذف شد.")
                }
            },
            onDismiss = { planToDelete = null }
        )
    }

    recordToDelete?.let { recordDisplay ->
        ConfirmDeleteDialog(
            title = "حذف سابقه سرویس",
            message = "آیا از حذف سابقه «${recordDisplay.itemName}» (${NumberUtil.formatKm(recordDisplay.record.km)}) اطمینان دارید؟",
            onConfirm = {
                viewModel.deleteRecord(recordDisplay.record)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    if (showAddItemDialog) {
        AddEditItemDialog(
            availableCategories = uiState.categories,
            onDismiss = { showAddItemDialog = false },
            onSave = { name, category, km, months, desc, _ ->
                viewModel.addItem(name, category, km, months, desc)
                showAddItemDialog = false
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
            TopAppBar(
                title = {
                    Text(
                        text = uiState.vehicle?.name ?: "داشبورد خودرو",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedTab != DashboardTab.STATUS) {
                            viewModel.setSelectedTab(DashboardTab.STATUS)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "تغییر خودرو")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "منو دیتابیس")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("پشتیبان‌گیری از کل دیتابیس (Export)") },
                                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.exportDatabase(
                                        onSuccess = {
                                            scope.launch { snackbarHostState.showSnackbar("فایل پشتیبان دیتابیس با موفقیت آماده شد.") }
                                        },
                                        onError = { err ->
                                            scope.launch { snackbarHostState.showSnackbar(err) }
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("بازیابی کل دیتابیس (Import)") },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showImportConfirmDialog = true
                                }
                            )
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                LoadingState()
            } else {
                val vehicle = uiState.vehicle
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header card (Always visible on top of all tabs)
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        VehicleMileageHeaderCard(
                            vehicleName = vehicle?.name ?: "",
                            year = vehicle?.year,
                            currentKm = vehicle?.currentKm ?: 0L,
                            onUpdateKm = {
                                kmInput = NumberUtil.formatKmInput(vehicle?.currentKm?.toString() ?: "")
                                showKmDialog = true
                            }
                        )
                    }

                    // Modern Tab navigation bar (Fixed 5-column single row)
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    ) {
                        DashboardTab.values().forEach { tab ->
                            val selected = selectedTab == tab
                            Tab(
                                selected = selected,
                                onClick = { viewModel.setSelectedTab(tab) },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab == DashboardTab.PLANS && uiState.pendingPlans.isNotEmpty()) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                                ) {
                                                    Text(
                                                        NumberUtil.toPersianDigits(uiState.pendingPlans.size),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else if (tab == DashboardTab.STATUS && uiState.overdueItems.isNotEmpty()) {
                                                Badge(
                                                    containerColor = StatusOverdue,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        NumberUtil.toPersianDigits(uiState.overdueItems.size),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    // Content based on Selected Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }

                        when (selectedTab) {
                            DashboardTab.STATUS -> {
                                // 1. Monthly Expense Summary Card
                                item {
                                    MonthlyExpenseCard(
                                        amount = uiState.monthlyTotal,
                                        onClick = { viewModel.setSelectedTab(DashboardTab.EXPENSES) }
                                    )
                                }

                                // 2. Overdue Services Section
                                if (uiState.overdueItems.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = "سرویس‌های عقب‌افتاده (${NumberUtil.toPersianDigits(uiState.overdueItems.size)})"
                                        )
                                    }
                                    items(uiState.overdueItems, key = { "overdue_${it.itemId}" }) { item ->
                                        ServiceStatusCard(
                                            item = item,
                                            currentKm = vehicle?.currentKm ?: 0L,
                                            onRecordService = { onNavigateToAddRecord(item.itemId, null) }
                                        )
                                    }
                                }

                                // 3. Upcoming Services Section
                                if (uiState.upcomingItems.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = "سرویس‌های نزدیک به موعد (${NumberUtil.toPersianDigits(uiState.upcomingItems.size)})"
                                        )
                                    }
                                    items(uiState.upcomingItems, key = { "upcoming_${it.itemId}" }) { item ->
                                        ServiceStatusCard(
                                            item = item,
                                            currentKm = vehicle?.currentKm ?: 0L,
                                            onRecordService = { onNavigateToAddRecord(item.itemId, null) }
                                        )
                                    }
                                }

                                // Healthy state
                                if (uiState.overdueItems.isEmpty() && uiState.upcomingItems.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = StatusOk,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "وضعیت خودرو عالی است!",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StatusOk
                                                    )
                                                    Text(
                                                        text = "در حال حاضر هیچ سرویسی سررسید نشده یا به موعد نرسیده است.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. Recent Records preview
                                item {
                                    SectionHeader(
                                        title = "آخرین سوابق ثبت‌شده",
                                        actionText = "مشاهده همه",
                                        onAction = { viewModel.setSelectedTab(DashboardTab.HISTORY) }
                                    )
                                }

                                if (uiState.recentRecords.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "هنوز سابقه‌ای برای این خودرو ثبت نشده است.",
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(uiState.recentRecords, key = { "rec_${it.record.id}" }) { recordDisplay ->
                                        RecentRecordCard(recordDisplay = recordDisplay)
                                    }
                                }
                            }

                            DashboardTab.PLANS -> {
                                if (uiState.pendingPlans.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon = Icons.Default.EventAvailable,
                                            title = "هیچ برنامه آینده‌ای ثبت نشده است",
                                            description = "با ثبت برنامه آینده می‌توانید برای تعویض روغن، تسمه تایم، لنت یا سرویس‌های دوره‌ای یادآور کیلومتری یا زمانی تعیین کنید.",
                                            actionText = "ثبت اولین برنامه آینده",
                                            onAction = { onNavigateToAddPlan(null, null) }
                                        )
                                    }
                                } else {
                                    item {
                                        SectionHeader(
                                            title = "لیست برنامه‌های آینده (${NumberUtil.toPersianDigits(uiState.pendingPlans.size)})",
                                            actionText = "افزودن برنامه",
                                            onAction = { onNavigateToAddPlan(null, null) }
                                        )
                                    }

                                    items(uiState.pendingPlans, key = { "plan_${it.plan.id}" }) { planDisplay ->
                                        PlannedServiceCard(
                                            planDisplay = planDisplay,
                                            onConvert = {
                                                onNavigateToAddRecord(planDisplay.plan.itemId, planDisplay.plan.id)
                                            },
                                            onEdit = {
                                                onNavigateToAddPlan(null, planDisplay.plan.id)
                                            },
                                            onDelete = {
                                                planToDelete = planDisplay
                                            }
                                        )
                                    }
                                }
                            }

                            DashboardTab.HISTORY -> {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { onNavigateToAddRecord(null, null) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                "ثبت سرویس",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onNavigateToBulkRecord(null) },
                                            modifier = Modifier
                                                .weight(1.25f)
                                                .defaultMinSize(minHeight = 44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                "ثبت دسته‌جمعی",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = { viewModel.exportRecordsToCsv() },
                                            modifier = Modifier
                                                .weight(0.85f)
                                                .defaultMinSize(minHeight = 44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color(0xFFE8F5E9),
                                                contentColor = Color(0xFF1B5E20)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                "خروجی",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                val pageSize = 20
                                val totalRecords = uiState.allRecords.size
                                val totalPages = maxOf(1, (totalRecords + pageSize - 1) / pageSize)
                                val safePage = historyPage.coerceIn(1, totalPages)
                                val startIndex = (safePage - 1) * pageSize
                                val endIndex = minOf(startIndex + pageSize, totalRecords)
                                val currentRecords = if (totalRecords > 0) uiState.allRecords.subList(startIndex, endIndex) else emptyList()

                                item {
                                    SectionHeader(
                                        title = "سوابق ثبت‌شده خودرو (${NumberUtil.toPersianDigits(totalRecords)})"
                                    )
                                }

                                if (uiState.allRecords.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon = Icons.Default.History,
                                            title = "هیچ سابقه سرویسی وجود ندارد",
                                            description = "برای این خودرو هنوز سابقه سرویسی ثبت نشده است.",
                                            actionText = "ثبت اولین سابقه سرویس",
                                            onAction = { onNavigateToAddRecord(null, null) }
                                        )
                                    }
                                } else {
                                    items(currentRecords, key = { "rec_${it.record.id}" }) { recordDisplay ->
                                        RecentRecordCard(
                                            recordDisplay = recordDisplay,
                                            onClick = { onNavigateToEditRecord(recordDisplay.record.id) },
                                            onDelete = { recordToDelete = recordDisplay }
                                        )
                                    }

                                    if (totalPages > 1) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { if (safePage > 1) historyPage = safePage - 1 },
                                                        enabled = safePage > 1,
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("صفحه قبلی", fontSize = 12.sp)
                                                    }

                                                    Text(
                                                        text = "صفحه ${NumberUtil.toPersianDigits(safePage)} از ${NumberUtil.toPersianDigits(totalPages)}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    OutlinedButton(
                                                        onClick = { if (safePage < totalPages) historyPage = safePage + 1 },
                                                        enabled = safePage < totalPages,
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("صفحه بعدی", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            DashboardTab.ITEMS -> {
                                item {
                                    Button(
                                        onClick = { showAddItemDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "افزودن قلم سرویس جدید",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (uiState.categories.isNotEmpty()) {
                                    item {
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = selectedCategory == null,
                                                    onClick = { selectedCategory = null },
                                                    label = { Text("همه دسته‌ها") }
                                                )
                                            }
                                            items(uiState.categories) { cat ->
                                                FilterChip(
                                                    selected = selectedCategory == cat,
                                                    onClick = {
                                                        selectedCategory = if (selectedCategory == cat) null else cat
                                                    },
                                                    label = { Text(cat) }
                                                )
                                            }
                                        }
                                    }
                                }

                                val filteredItems = if (selectedCategory == null) {
                                    uiState.allItems
                                } else {
                                    uiState.allItems.filter { it.category == selectedCategory }
                                }

                                item {
                                    SectionHeader(
                                        title = "اقلام و قطعات سرویس (${NumberUtil.toPersianDigits(filteredItems.size)})"
                                    )
                                }

                                if (filteredItems.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon = Icons.Default.Build,
                                            title = "هیچ قلمی یافت نشد",
                                            description = "برای این خودرو هنوز قلم تعمیراتی در این دسته‌بندی ثبت نشده است.",
                                            actionText = "افزودن قلم جدید",
                                            onAction = { showAddItemDialog = true }
                                        )
                                    }
                                } else {
                                    items(filteredItems, key = { "item_${it.id}" }) { item ->
                                        ItemRowCard(
                                            item = item,
                                            onRecord = { onNavigateToAddRecord(item.id, null) },
                                            onHistory = onNavigateToHistory,
                                            onBulkRecord = { onNavigateToBulkRecord(item.id) },
                                            onEdit = { itemToEdit = item },
                                            onDelete = { itemToDelete = item }
                                        )
                                    }
                                }
                            }

                            DashboardTab.EXPENSES -> {
                                item {
                                    MonthlyExpenseCard(
                                        amount = uiState.monthlyTotal,
                                        onClick = onNavigateToExpenses
                                    )
                                }

                                item {
                                    Button(
                                        onClick = onNavigateToExpenses,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("گزارش تحلیلی و نمودار هزینه‌ها", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "راهنمای هزینه‌ها",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "در صفحه کامل گزارش هزینه‌ها، می‌توانید تفکیک هزینه‌ها بر اساس قطعه، دسته‌بندی و بازه زمانی را به همراه نمودار مشاهده کرده و خروجی اکسل دریافت کنید.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(36.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleMileageHeaderCard(
    vehicleName: String,
    year: Int?,
    currentKm: Long,
    onUpdateKm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicleName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (year != null) {
                        Text(
                            text = "مدل: ${NumberUtil.toPersianDigits(year.toString())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onUpdateKm,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ویرایش کیلومتر", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "کیلومتر فعلی:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = NumberUtil.formatKm(currentKm),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun ActionShortcutsRow(
    onAddRecord: () -> Unit,
    onAddPlan: () -> Unit,
    onHistory: () -> Unit,
    onItems: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShortcutButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AddCircle,
            label = "ثبت سرویس",
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            onClick = onAddRecord
        )
        ShortcutButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Event,
            label = "برنامه آینده",
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            onClick = onAddPlan
        )
        ShortcutButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.History,
            label = "سوابق",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onHistory
        )
        ShortcutButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Build,
            label = "اقلام سرویس",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onItems
        )
    }
}

@Composable
fun ShortcutButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MonthlyExpenseCard(
    amount: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "هزینه‌های ماه جاری (شمسی)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${NumberUtil.formatCurrency(amount)} تومان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "مشاهده گزارش",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ServiceStatusCard(
    item: ItemWithLastRecord,
    currentKm: Long,
    onRecordService: () -> Unit
) {
    val isOverdue = item.status == ServiceStatus.OVERDUE
    val borderColor = if (isOverdue) StatusOverdue else StatusUpcoming

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = item.status)
            }

            Spacer(Modifier.height(8.dp))

            // Reason / Targets
            if (item.nextKm != null) {
                val diffKm = item.nextKm - currentKm
                val kmLabel = if (diffKm < 0) {
                    "${NumberUtil.formatKm(-diffKm)} گذشته از موعد"
                } else {
                    "${NumberUtil.formatKm(diffKm)} تا موعد بعدی (${NumberUtil.formatKm(item.nextKm)})"
                }
                Text(
                    text = "کیلومتر هدف: $kmLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (diffKm < 0) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (item.nextDate != null) {
                val diffDays = (item.nextDate - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)
                val dateLabel = if (diffDays < 0) {
                    "${NumberUtil.toPersianDigits(-diffDays.toInt())} روز گذشته از موعد (${JalaliDateUtil.timestampToJalaliString(item.nextDate)})"
                } else {
                    "${NumberUtil.toPersianDigits(diffDays.toInt())} روز مانده (${JalaliDateUtil.timestampToJalaliString(item.nextDate)})"
                }
                Text(
                    text = "تاریخ هدف: $dateLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (diffDays < 0) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onRecordService,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverdue) StatusOverdue else StatusUpcoming
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ثبت انجام سرویس", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlannedServiceCard(
    planDisplay: PlannedServiceDisplay,
    onConvert: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val plan = planDisplay.plan
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BuildCircle,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = planDisplay.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            if (plan.targetKm != null) {
                Text(
                    text = "کیلومتر هدف: ${NumberUtil.formatKm(plan.targetKm)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (plan.targetDate != null) {
                Text(
                    text = "تاریخ هدف: ${JalaliDateUtil.timestampToJalaliStringWithMonthName(plan.targetDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plan.notes.isNotBlank() && !plan.notes.contains("محاسبه خودکار")) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "یادداشت: ${plan.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف برنامه",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Edit button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "ویرایش برنامه",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Convert button
                FilledTonalButton(
                    onClick = onConvert,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("انجام شد", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecentRecordCard(
    recordDisplay: RecordDisplay,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recordDisplay.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                val kmText = NumberUtil.formatKm(recordDisplay.record.km)
                val subtitle = if (recordDisplay.dateDisplay.isNotBlank()) {
                    "${recordDisplay.dateDisplay} • $kmText"
                } else {
                    "کارکرد: $kmText"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (recordDisplay.record.cost != null) {
                    CostChip(amount = recordDisplay.record.cost)
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف سابقه",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
