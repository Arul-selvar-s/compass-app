package com.compass.diary.ui.screens.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.data.local.entity.ReminderEntity
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val upcoming  by viewModel.upcomingReminders.collectAsState()
    val completed by viewModel.completedReminders.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Reminders", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = CompassColors.Blue600
            ) { Icon(Icons.Default.Add, "Add reminder", tint = androidx.compose.ui.graphics.Color.White) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Upcoming (${upcoming.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Completed") })
            }

            when (selectedTab) {
                0 -> ReminderList(reminders = upcoming, isCompleted = false, viewModel = viewModel)
                1 -> ReminderList(reminders = completed, isCompleted = true, viewModel = viewModel)
            }
        }
    }

    if (showAdd) {
        AddReminderSheet(
            onAdd = { reminder ->
                viewModel.addReminder(reminder)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun ReminderList(
    reminders: List<ReminderEntity>,
    isCompleted: Boolean,
    viewModel: DiaryViewModel
) {
    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔔", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isCompleted) "No completed reminders" else "No upcoming reminders",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder  = reminder,
                    onComplete = { viewModel.markReminderComplete(reminder.id) },
                    onDelete   = { viewModel.deleteReminder(reminder) }
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (reminder.note.isNotBlank()) {
                    Text(reminder.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp),
                        tint = CompassColors.Blue400)
                    Spacer(Modifier.width(4.dp))
                    Text(df.format(Date(reminder.triggerAt)),
                        style = MaterialTheme.typography.labelSmall, color = CompassColors.Blue400)
                    Spacer(Modifier.width(8.dp))
                    RepeatBadge(type = reminder.repeatType)
                }
            }
            if (!reminder.isCompleted) {
                IconButton(onClick = onComplete) { Icon(Icons.Default.CheckCircleOutline, "Complete") }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = CompassColors.Error)
            }
        }
    }
}

@Composable
private fun RepeatBadge(type: String) {
    val (icon, label) = when (type) {
        "DAILY"   -> Icons.Default.Repeat to "Daily"
        "WEEKLY"  -> Icons.Default.Repeat to "Weekly"
        "MONTHLY" -> Icons.Default.Repeat to "Monthly"
        else      -> null to null
    }
    if (icon != null && label != null) {
        AssistChip(
            onClick = {},
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(icon, null, modifier = Modifier.size(12.dp)) },
            modifier = Modifier.height(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderSheet(
    onAdd: (ReminderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note  by remember { mutableStateOf("") }
    var repeatType by remember { mutableStateOf("ONCE") }
    // Simple: default to 1 hour from now
    val defaultTrigger = remember { System.currentTimeMillis() + 3600_000L }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("New Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3
            )
            Spacer(Modifier.height(12.dp))
            Text("Repeat", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ONCE","DAILY","WEEKLY","MONTHLY").forEach { type ->
                    FilterChip(
                        selected = repeatType == type,
                        onClick  = { repeatType = type },
                        label    = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(ReminderEntity(
                            title = title, note = note,
                            repeatType = repeatType, triggerAt = defaultTrigger
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CompassColors.Blue600)
            ) { Text("Save Reminder") }
            Spacer(Modifier.height(32.dp))
        }
    }
}
