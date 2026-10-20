package ir.alidevcore.autocare.ui.expenses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alidevcore.autocare.data.dao.CategoryCost
import ir.alidevcore.autocare.ui.components.AutoCareTopBar
import ir.alidevcore.autocare.ui.components.LoadingState
import ir.alidevcore.autocare.ui.theme.Primary
import ir.alidevcore.autocare.ui.theme.Secondary
import ir.alidevcore.autocare.util.NumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesReportScreen(
    viewModel: ExpensesViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "گزارش و تحلیل هزینه‌ها",
                onNavigateBack = onNavigateBack
            )
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
                    Spacer(Modifier.height(8.dp))
                    // Period Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodTab(
                            title = "کل دوره",
                            selected = uiState.period == ExpensePeriod.ALL_TIME,
                            onClick = { viewModel.onPeriodChanged(ExpensePeriod.ALL_TIME) },
                            modifier = Modifier.weight(1f)
                        )
                        PeriodTab(
                            title = "سال جاری",
                            selected = uiState.period == ExpensePeriod.THIS_YEAR,
                            onClick = { viewModel.onPeriodChanged(ExpensePeriod.THIS_YEAR) },
                            modifier = Modifier.weight(1f)
                        )
                        PeriodTab(
                            title = "ماه جاری",
                            selected = uiState.period == ExpensePeriod.THIS_MONTH,
                            onClick = { viewModel.onPeriodChanged(ExpensePeriod.THIS_MONTH) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Total Expense Summary Card
                item {
                    TotalExpenseCard(
                        totalAmount = uiState.totalCost,
                        costedCount = uiState.recordsWithCostCount,
                        totalCount = uiState.totalRecordsCount
                    )
                }

                // Monthly Spending Bar Chart
                if (uiState.monthlyBars.any { it.amount > 0 }) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "روند هزینه‌های ۶ ماه گذشته",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(16.dp))
                                MonthlyBarChart(
                                    data = uiState.monthlyBars,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                        }
                    }
                }

                // Category Breakdown Header
                item {
                    Text(
                        text = "تفکیک هزینه‌ها بر اساس دسته‌بندی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.categoryCosts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "در این بازه زمانی، هزینه‌ای ثبت نشده است.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.categoryCosts, key = { it.category }) { catCost ->
                        CategoryCostRow(
                            categoryCost = catCost,
                            totalCost = uiState.totalCost
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun PeriodTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = modifier
    )
}

@Composable
fun TotalExpenseCard(
    totalAmount: Long,
    costedCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجموع هزینه‌های ثبت‌شده",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleSmall
                )
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${NumberUtil.formatCurrency(totalAmount)} تومان",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "محاسبه‌شده از ${NumberUtil.toPersianDigits(costedCount)} سابقه دارای مبلغ (از مجموع ${NumberUtil.toPersianDigits(totalCount)} سابقه)",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    data: List<MonthlyBarData>,
    modifier: Modifier = Modifier
) {
    val maxAmount = (data.maxOfOrNull { it.amount } ?: 1L).coerceAtLeast(1L)
    val primaryColor = Primary
    val secondaryColor = Secondary

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            val ratio = (item.amount.toFloat() / maxAmount.toFloat()).coerceIn(0.04f, 1f)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (item.amount > 0) {
                    Text(
                        text = NumberUtil.toPersianDigits(item.amount / 1000) + "ه",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(0.72f * ratio)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (item.amount > 0) primaryColor else MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CategoryCostRow(
    categoryCost: CategoryCost,
    totalCost: Long
) {
    val percentage = if (totalCost > 0) (categoryCost.totalCost.toFloat() / totalCost.toFloat()) else 0f
    val percentInt = (percentage * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryCost.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${NumberUtil.formatCurrency(categoryCost.totalCost)} تومان",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${NumberUtil.toPersianDigits(categoryCost.recordCount)} مورد ثبت‌شده",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${NumberUtil.toPersianDigits(percentInt)}٪ کل",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
