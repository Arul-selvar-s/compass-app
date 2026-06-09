package com.compass.diary.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.data.local.entity.DiaryEntryEntity
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryHomeScreen(
    onOpenPage:      (String) -> Unit,
    onOpenCalendar:  () -> Unit,
    onOpenStarred:   () -> Unit,
    onOpenSearch:    () -> Unit,
    onOpenAI:        () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings:  () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val entries  by viewModel.allEntries.collectAsState()
    val todayKey by viewModel.todayKey.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Compass", fontWeight = FontWeight.Bold)
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch)    { Icon(Icons.Default.Search, "Search") }
                    IconButton(onClick = onOpenSettings)  { Icon(Icons.Default.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenPage(todayKey) },
                icon = { Icon(Icons.Default.Edit, "Today") },
                text = { Text("Today") },
                containerColor = CompassColors.Blue600,
                contentColor = Color.White
            )
        },
        bottomBar = {
            BottomNavBar(
                onCalendar  = onOpenCalendar,
                onStarred   = onOpenStarred,
                onAI        = onOpenAI,
                onReminders = onOpenReminders
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick stats
            item {
                QuickStats(entryCount = entries.size, viewModel = viewModel)
            }

            // Today's entry or prompt
            item {
                TodayCard(
                    dateKey = todayKey,
                    entry   = entries.find { it.dateKey == todayKey },
                    onClick = { onOpenPage(todayKey) }
                )
            }

            // Past entries
            if (entries.isNotEmpty()) {
                item {
                    Text(
                        "Past Pages",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(
                    items = entries.filter { it.dateKey != todayKey },
                    key   = { it.dateKey }
                ) { entry ->
                    DiaryEntryCard(entry = entry, onClick = { onOpenPage(entry.dateKey) })
                }
            }
        }
    }
}

@Composable
private fun QuickStats(entryCount: Int, viewModel: DiaryViewModel) {
    val wordCount by viewModel.totalWordCount.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(label = "Pages", value = "$entryCount", modifier = Modifier.weight(1f))
        StatChip(label = "Words", value = "$wordCount", modifier = Modifier.weight(1f))
        StatChip(
            label = "Streak",
            value = "${viewModel.streakDays}d",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayCard(dateKey: String, entry: DiaryEntryEntity?, onClick: () -> Unit) {
    val hasContent = entry?.plainText?.isNotBlank() == true
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CompassColors.Blue800)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = CompassColors.Blue300
                )
                Text(
                    if (hasContent) entry!!.title else "Start writing…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (hasContent) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry!!.plainText.take(120),
                        style = MaterialTheme.typography.bodySmall,
                        color = CompassColors.Blue300,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                if (hasContent) Icons.Default.Edit else Icons.Default.AddCircleOutline,
                contentDescription = null,
                tint = CompassColors.Blue300,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun DiaryEntryCard(entry: DiaryEntryEntity, onClick: () -> Unit) {
    val date = try {
        java.time.LocalDate.parse(entry.dateKey)
            .format(DateTimeFormatter.ofPattern("dd MMM"))
    } catch (e: Exception) { entry.dateKey }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Date badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CompassColors.Blue400
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (entry.plainText.isNotBlank()) {
                    Text(
                        entry.plainText.take(100),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${entry.wordCount} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.isShared) {
                Icon(Icons.Default.Share, null, tint = CompassColors.Blue400, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    onCalendar: () -> Unit,
    onStarred: () -> Unit,
    onAI: () -> Unit,
    onReminders: () -> Unit
) {
    data class NavItem(val icon: ImageVector, val label: String, val action: () -> Unit)
    val items = listOf(
        NavItem(Icons.Default.CalendarMonth, "Calendar", onCalendar),
        NavItem(Icons.Default.Star, "Starred", onStarred),
        NavItem(Icons.Default.AutoAwesome, "AI", onAI),
        NavItem(Icons.Default.Notifications, "Reminders", onReminders),
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            NavigationBarItem(
                icon    = { Icon(item.icon, item.label) },
                label   = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                selected = false,
                onClick = item.action
            )
        }
    }
}
