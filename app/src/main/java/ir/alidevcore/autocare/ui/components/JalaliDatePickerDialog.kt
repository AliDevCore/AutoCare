package ir.alidevcore.autocare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.alidevcore.autocare.util.JalaliDateUtil

@Composable
fun JalaliDatePickerDialog(
    initialTimestamp: Long = System.currentTimeMillis(),
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialJalali = JalaliDateUtil.toJalali(initialTimestamp)
    var selectedYear by remember { mutableIntStateOf(initialJalali.year) }
    var selectedMonth by remember { mutableIntStateOf(initialJalali.month) }
    var selectedDay by remember { mutableIntStateOf(initialJalali.day) }
    var displayYear by remember { mutableIntStateOf(initialJalali.year) }
    var displayMonth by remember { mutableIntStateOf(initialJalali.month) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "انتخاب تاریخ",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Month/Year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (displayMonth == 1) {
                            displayMonth = 12
                            displayYear--
                        } else {
                            displayMonth--
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                    }

                    Text(
                        text = "${JalaliDateUtil.getJalaliMonthName(displayMonth)} ${JalaliDateUtil.toPersianDigits(displayYear)}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = {
                        if (displayMonth == 12) {
                            displayMonth = 1
                            displayYear++
                        } else {
                            displayMonth++
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Day names header - Jalali week starts Saturday
                val dayNames = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayNames.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Compute first day of month offset
                // Jalali week starts from Saturday
                val firstDayTimestamp = JalaliDateUtil.jalaliToTimestamp(displayYear, displayMonth, 1)
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = firstDayTimestamp
                // Cal day of week: 1=Sunday, 2=Monday ... 7=Saturday
                // In Jalali calendar: Saturday=0, Sunday=1, ... Friday=6
                val gregDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val offset = when (gregDayOfWeek) {
                    java.util.Calendar.SATURDAY -> 0
                    java.util.Calendar.SUNDAY -> 1
                    java.util.Calendar.MONDAY -> 2
                    java.util.Calendar.TUESDAY -> 3
                    java.util.Calendar.WEDNESDAY -> 4
                    java.util.Calendar.THURSDAY -> 5
                    java.util.Calendar.FRIDAY -> 6
                    else -> 0
                }
                val monthLength = JalaliDateUtil.getJalaliMonthLength(displayYear, displayMonth)
                val totalCells = offset + monthLength
                val rows = (totalCells + 6) / 7

                // Grid of days
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - offset + 1
                            val isValid = day in 1..monthLength
                            val isSelected = isValid && day == selectedDay &&
                                    displayMonth == selectedMonth && displayYear == selectedYear
                            val isToday = run {
                                val (ty, tm, td) = JalaliDateUtil.getCurrentJalaliDate()
                                isValid && day == td && displayMonth == tm && displayYear == ty
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (isValid) Modifier.clickable {
                                            selectedDay = day
                                            selectedMonth = displayMonth
                                            selectedYear = displayYear
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isValid) {
                                    Text(
                                        text = JalaliDateUtil.toPersianDigits(day),
                                        fontSize = 14.sp,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Button(onClick = {
                        val timestamp = JalaliDateUtil.jalaliToTimestamp(selectedYear, selectedMonth, selectedDay)
                        onDateSelected(timestamp)
                        onDismiss()
                    }) {
                        Text("تأیید")
                    }
                }
            }
        }
    }
}
