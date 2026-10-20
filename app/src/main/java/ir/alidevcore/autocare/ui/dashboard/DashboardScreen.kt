package ir.alidevcore.autocare.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddRecord: (itemId: Long?, planId: Long?) -> Unit,
    onNavigateToAddPlan: (itemId: Long?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showKmDialog by remember { mutableStateOf(false) }
    var showKmDecreaseWarning by remember { mutableStateOf<Long?>(null) }
    var kmInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.showKmUpdateSuccess) {
        if (uiState.showKmUpdateSuccess) {
            snackbarHostState.showSnackbar("کیلومتر خودرو به‌روزرسانی شد")
            viewModel.clearKmUpdateSuccess()
        }
    }

    if (showKmDecreaseWarning != null) {
        AlertDialog(
            onDismissRequest = { showKmDecreaseWarning = null },
            title = { Text("هشدار کاهش کیلومتر") },
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
        AlertDialog(
            onDismissRequest = { showKmDialog = false },
            title = { Text("به‌روزرسانی کیلومتر فعلی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "کیلومتر فعلی: ${NumberUtil.formatKm(uiState.vehicle?.currentKm ?: 0L)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = kmInput,
                        onValueChange = { kmInput = it },
                        label = { Text("کیلومتر جدید") },
                        placeholder = { Text("مثلاً: ۱۳۲۰۰۰") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = NumberUtil.parsePersianNumber(kmInput)
                    if (parsed != null && parsed >= 0) {
                        val current = uiState.vehicle?.currentKm ?: 0L
                        if (parsed < current) {
                            showKmDecreaseWarning = parsed
                        } else {
                            viewModel.updateCurrentKm(parsed)
                            showKmDialog = false
                        }
                    }
                }) {
                    Text("ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.vehicle?.name ?: "داشبورد خودرو",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "تغییر خودرو")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToExport) {
                        Icon(Icons.Default.Share, contentDescription = "خروجی")
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        // 1. Vehicle Header & Mileage Card
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

                    // 2. Action Shortcuts
                    item {
                        ActionShortcutsRow(
                            onAddRecord = { onNavigateToAddRecord(null, null) },
                            onAddPlan = { onNavigateToAddPlan(null) },
                            onHistory = onNavigateToHistory,
                            onItems = onNavigateToItems
                        )
                    }

                    // 3. Monthly Expense Summary Card
                    item {
                        MonthlyExpenseCard(
                            amount = uiState.monthlyTotal,
                            onClick = onNavigateToExpenses
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

                    // 6. Future Planned Services
                    if (uiState.pendingPlans.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "برنامه‌های آینده (${NumberUtil.toPersianDigits(uiState.pendingPlans.size)})",
                                actionText = "افزودن",
                                onAction = { onNavigateToAddPlan(null) }
                            )
                        }
                        items(uiState.pendingPlans, key = { "plan_${it.id}" }) { plan ->
                            PlannedServiceCard(
                                plan = plan,
                                onConvert = { onNavigateToAddRecord(plan.itemId, plan.id) }
                            )
                        }
                    }

                    // 7. Recent Records
                    item {
                        SectionHeader(
                            title = "آخرین سوابق ثبت‌شده",
                            actionText = "مشاهده همه",
                            onAction = onNavigateToHistory
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

                    item {
                        Spacer(Modifier.height(32.dp))
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
        Column(modifier = Modifier.padding(20.dp)) {
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
                    Text("ویرایش کیلومتر", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
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
                fontSize = 11.sp,
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
                        .size(40.dp)
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
                Icons.Default.ArrowBack,
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = if (diffKm < 0) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant
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
                    style = MaterialTheme.typography.bodySmall,
                    color = if (diffDays < 0) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ثبت انجام سرویس", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PlannedServiceCard(
    plan: PlannedService,
    onConvert: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "برنامه سرویس آینده",
                        style = MaterialTheme.typography.titleSmall,
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
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (plan.targetKm != null) {
                Text(
                    text = "کیلومتر هدف: ${NumberUtil.formatKm(plan.targetKm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plan.targetDate != null) {
                Text(
                    text = "تاریخ هدف: ${JalaliDateUtil.timestampToJalaliStringWithMonthName(plan.targetDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plan.notes.isNotBlank()) {
                Text(
                    text = "یادداشت: ${plan.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onConvert,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تبدیل به سرویس انجام‌شده", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RecentRecordCard(recordDisplay: RecordDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (recordDisplay.record.cost != null) {
                CostChip(amount = recordDisplay.record.cost)
            }
        }
    }
}
