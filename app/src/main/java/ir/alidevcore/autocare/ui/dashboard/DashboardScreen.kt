package ir.alidevcore.autocare.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import ir.alidevcore.autocare.data.model.PlannedService
import ir.alidevcore.autocare.data.model.relations.ItemWithLastRecord
import ir.alidevcore.autocare.data.model.relations.ServiceStatus
import ir.alidevcore.autocare.ui.components.*
import ir.alidevcore.autocare.ui.theme.*
import ir.alidevcore.autocare.util.JalaliDateUtil
import ir.alidevcore.autocare.util.NumberUtil
import kotlinx.coroutines.launch

enum class DashboardTab(val title: String, val icon: ImageVector) {
    STATUS("داشبورد", Icons.Default.DirectionsCar),
    PLANS("برنامه آینده", Icons.Default.Event),
    HISTORY("سوابق", Icons.Default.History),
    ITEMS("اقلام سرویس", Icons.Default.Build),
    EXPENSES("هزینه‌ها", Icons.Default.MonetizationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddRecord: (itemId: Long?, planId: Long?) -> Unit,
    onNavigateToAddPlan: (itemId: Long?, planId: Long?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToBulkRecord: (itemId: Long?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(DashboardTab.STATUS) }
    var showKmDialog by remember { mutableStateOf(false) }
    var showKmDecreaseWarning by remember { mutableStateOf<Long?>(null) }
    var kmInput by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<PlannedServiceDisplay?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                        onValueChange = { kmInput = it },
                        label = { Text("کیلومتر جدید") },
                        placeholder = { Text("مثال: ۱۲۵۰۰۰") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newKm = kmInput.trim().toLongOrNull()
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
                    IconButton(onClick = onNavigateBack) {
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
                                kmInput = vehicle?.currentKm?.toString() ?: ""
                                showKmDialog = true
                            }
                        )
                    }

                    // Modern Tab navigation bar
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 16.dp,
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
                                onClick = { selectedTab = tab },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tab.title,
                                            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (tab == DashboardTab.PLANS && uiState.pendingPlans.isNotEmpty()) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.secondary
                                            ) {
                                                Text(
                                                    text = NumberUtil.toPersianDigits(uiState.pendingPlans.size),
                                                    color = MaterialTheme.colorScheme.onSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        } else if (tab == DashboardTab.STATUS && uiState.overdueItems.isNotEmpty()) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = StatusOverdue
                                            ) {
                                                Text(
                                                    text = NumberUtil.toPersianDigits(uiState.overdueItems.size),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
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
                                // 1. Action Shortcuts to switch or perform actions
                                item {
                                    ActionShortcutsRow(
                                        onAddRecord = { onNavigateToAddRecord(null, null) },
                                        onAddPlan = { selectedTab = DashboardTab.PLANS },
                                        onHistory = { selectedTab = DashboardTab.HISTORY },
                                        onItems = { selectedTab = DashboardTab.ITEMS }
                                    )
                                }

                                // 2. Quick Historical Records Bulk Entry
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToBulkRecord(null) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.tertiary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onTertiary
                                                    )
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "ثبت دسته‌جمعی سوابق گذشته",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                    Text(
                                                        text = "ورود سریع سوابق گذشته (مانند شمع یا لنت در کیلومترهای مختلف)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }

                                // 3. Monthly Expense Summary Card
                                item {
                                    MonthlyExpenseCard(
                                        amount = uiState.monthlyTotal,
                                        onClick = { selectedTab = DashboardTab.EXPENSES }
                                    )
                                }

                                // 4. Overdue Services Section
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

                                // 5. Upcoming Services Section
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

                                // 6. Recent Records preview
                                item {
                                    SectionHeader(
                                        title = "آخرین سوابق ثبت‌شده",
                                        actionText = "مشاهده همه سوابق",
                                        onAction = { selectedTab = DashboardTab.HISTORY }
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
                                // Add Plan Prominent Button
                                item {
                                    Button(
                                        onClick = { onNavigateToAddPlan(null, null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "برنامه‌ریزی سرویس آینده جدید",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onNavigateToAddRecord(null, null) },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("ثبت سرویس جدید", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { onNavigateToBulkRecord(null) },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("ثبت دسته‌جمعی", fontSize = 14.sp)
                                        }
                                    }
                                }

                                item {
                                    FilledTonalButton(
                                        onClick = onNavigateToHistory,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("جستجو، فیلتر و خروجی اکسل در صفحه کامل سوابق", fontSize = 14.sp)
                                    }
                                }

                                item {
                                    SectionHeader(
                                        title = "آخرین سوابق ثبت‌شده خودرو",
                                        actionText = "صفحه کامل سوابق",
                                        onAction = onNavigateToHistory
                                    )
                                }

                                if (uiState.recentRecords.isEmpty()) {
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
                                    items(uiState.recentRecords, key = { "rec_${it.record.id}" }) { recordDisplay ->
                                        RecentRecordCard(recordDisplay = recordDisplay)
                                    }
                                }
                            }

                            DashboardTab.ITEMS -> {
                                item {
                                    Button(
                                        onClick = onNavigateToItems,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("مدیریت و افزودن اقلام سرویس جدید", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                item {
                                    SectionHeader(
                                        title = "اقلام سرویس و قطعات فعال",
                                        actionText = "مشاهده تمام اقلام",
                                        onAction = onNavigateToItems
                                    )
                                }

                                val allItems = uiState.overdueItems + uiState.upcomingItems
                                if (allItems.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "اقلام سرویس استاندارد برای این خودرو فعال هستند. برای شخصی‌سازی دوره‌ها دکمه مدیریت را لمس کنید.",
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(allItems, key = { "item_stat_${it.itemId}" }) { item ->
                                        ServiceStatusCard(
                                            item = item,
                                            currentKm = vehicle?.currentKm ?: 0L,
                                            onRecordService = { onNavigateToAddRecord(item.itemId, null) }
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
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Analytics, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("مشاهده گزارش تحلیلی کامل هزینه‌ها و نمودارها", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "کیلومتر فعلی خودرو:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = NumberUtil.formatKm(currentKm),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "برنامه‌ریزی‌شده",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تبدیل به سرویس انجام‌شده", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecentRecordCard(recordDisplay: RecordDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column {
                Text(
                    text = recordDisplay.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${recordDisplay.dateDisplay} • ${NumberUtil.formatKm(recordDisplay.record.km)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (recordDisplay.record.cost != null) {
                CostChip(amount = recordDisplay.record.cost)
            }
        }
    }
}
