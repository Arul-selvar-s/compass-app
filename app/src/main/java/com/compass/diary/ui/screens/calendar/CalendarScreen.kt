package com.compass.diary.ui.screens.calendar

import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onOpenPage: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val allKeys by viewModel.allDateKeys.collectAsState()
    val entryKeySet = remember(allKeys) { allKeys.toSet() }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Calendar", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, "Previous month")
                }
                Text(
                    "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { currentMonth = currentMonth.plusMonths(1) },
                    enabled = currentMonth < YearMonth.now()
                ) {
                    Icon(Icons.Default.ChevronRight, "Next month")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar grid
            CalendarGrid(
                yearMonth = currentMonth,
                today = today,
                entryKeySet = entryKeySet,
                onDayClick = { date ->
                    val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    onOpenPage(key)
                }
            )

            Spacer(Modifier.height(24.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LegendItem(color = CompassColors.Blue500, label = "Has entry")
                LegendItem(color = CompassColors.Gold400, label = "Today")
            }

            Spacer(Modifier.height(16.dp))

            // Stats for current month
            MonthStats(
                yearMonth = currentMonth,
                entryKeySet = entryKeySet
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    today: LocalDate,
    entryKeySet: Set<String>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDay = yearMonth.atDay(1)
    // Monday-based: Mon=1...Sun=7, Java: Mon=1...Sun=7
    val startOffset = (firstDay.dayOfWeek.value - 1) // 0-indexed
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells  = startOffset + daysInMonth
    val rows        = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum    = cellIndex - startOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date   = yearMonth.atDay(dayNum)
                        val key    = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val isToday = date == today
                        val hasEntry = key in entryKeySet
                        val isFuture = date > today

                        DayCell(
                            day = dayNum,
                            isToday = isToday,
                            hasEntry = hasEntry,
                            isFuture = isFuture,
                            onClick = { if (!isFuture) onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    hasEntry: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(
                if (!isFuture) Modifier.clickable(onClick = onClick) else Modifier
            )
            .background(
                when {
                    isToday   -> CompassColors.Gold400
                    hasEntry  -> CompassColors.Blue500
                    else      -> Color.Transparent
                },
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$day",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday || hasEntry) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isFuture  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                isToday || hasEntry -> Color.White
                else      -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthStats(yearMonth: YearMonth, entryKeySet: Set<String>) {
    val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val entriesThisMonth = entryKeySet.count { it.startsWith(prefix) }
    val daysInMonth = yearMonth.lengthOfMonth()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = "$entriesThisMonth", label = "Entries")
            VerticalDivider(modifier = Modifier.height(40.dp))
            StatItem(value = "${daysInMonth - entriesThisMonth}", label = "Missed")
            VerticalDivider(modifier = Modifier.height(40.dp))
            StatItem(
                value = "${if (daysInMonth > 0) (entriesThisMonth * 100 / daysInMonth) else 0}%",
                label = "Consistency"
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
