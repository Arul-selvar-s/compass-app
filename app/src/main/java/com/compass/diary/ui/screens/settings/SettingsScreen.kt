package com.compass.diary.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkMode            by viewModel.darkMode.collectAsState()
    val notificationsOn     by viewModel.notificationsEnabled.collectAsState()
    val autoSync            by viewModel.autoSync.collectAsState()
    val googleAccount       by viewModel.googleAccount.collectAsState()
    val lastSync            by viewModel.lastSyncLabel.collectAsState()
    val apiKey              by viewModel.anthropicApiKey.collectAsState()
    var showApiKeyDialog    by remember { mutableStateOf(false) }
    var showChangePasscode  by remember { mutableStateOf(false) }
    var showAbout           by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // ── ACCOUNT ─────────────────────────────────────────────
            SettingsSection("Account") {
                AccountRow(email = googleAccount ?: "Not signed in")
                SettingsDivider()
                SettingsRow(
                    icon  = Icons.Default.Sync,
                    title = "Auto-sync to Google Drive",
                    subtitle = "Last sync: $lastSync",
                    trailing = {
                        Switch(checked = autoSync, onCheckedChange = viewModel::setAutoSync)
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon  = Icons.Default.CloudUpload,
                    title = "Sync now",
                    onClick = { viewModel.syncNow() }
                )
            }

            // ── SECURITY ─────────────────────────────────────────────
            SettingsSection("Security") {
                SettingsRow(
                    icon  = Icons.Default.Explore,
                    title = "Change compass lock",
                    subtitle = "Update your secret directions",
                    onClick = { showChangePasscode = true }
                )
                SettingsDivider()
                SettingsRow(
                    icon  = Icons.Default.Fingerprint,
                    title = "Biometric unlock",
                    trailing = {
                        val biometric by viewModel.biometricEnabled.collectAsState()
                        Switch(checked = biometric, onCheckedChange = viewModel::setBiometric)
                    }
                )
            }

            // ── AI ASSISTANT ──────────────────────────────────────────
            SettingsSection("AI Assistant") {
                SettingsRow(
                    icon  = Icons.Default.Key,
                    title = "Anthropic API Key",
                    subtitle = if (apiKey.isNullOrBlank()) "Not configured" else "••••••••${apiKey?.takeLast(4)}",
                    onClick = { showApiKeyDialog = true }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "About AI features",
                    subtitle = "Uses Claude Sonnet to search and summarise your diary",
                    onClick = {}
                )
            }

            // ── APPEARANCE ────────────────────────────────────────────
            SettingsSection("Appearance") {
                SettingsRow(
                    icon  = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = darkMode,
                    trailing = {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) { Text(darkMode) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("SYSTEM", "DARK", "LIGHT").forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = { viewModel.setDarkMode(mode); expanded = false }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // ── NOTIFICATIONS ─────────────────────────────────────────
            SettingsSection("Notifications") {
                SettingsRow(
                    icon  = Icons.Default.Notifications,
                    title = "Android notifications",
                    subtitle = "Show diary reminders in notification bar",
                    trailing = {
                        Switch(checked = notificationsOn, onCheckedChange = viewModel::setNotifications)
                    }
                )
            }

            // ── DATA ──────────────────────────────────────────────────
            SettingsSection("Data & Privacy") {
                SettingsRow(
                    icon  = Icons.Default.DeleteSweep,
                    title = "Export diary",
                    subtitle = "Export all pages as text files",
                    onClick = { viewModel.exportDiary() }
                )
                SettingsDivider()
                SettingsRow(
                    icon  = Icons.Default.Lock,
                    title = "Encryption",
                    subtitle = "All data is encrypted with SQLCipher AES-256",
                    onClick = {}
                )
                SettingsDivider()
                SettingsRow(
                    icon  = Icons.Default.PrivacyTip,
                    title = "Privacy",
                    subtitle = "No ads, no analytics, no tracking",
                    onClick = {}
                )
            }

            // ── ABOUT ─────────────────────────────────────────────────
            SettingsSection("About") {
                SettingsRow(
                    icon  = Icons.Default.Info,
                    title = "Compass",
                    subtitle = "Version 1.0.0",
                    onClick = { showAbout = true }
                )
            }

            // ── LOGOUT ────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { viewModel.logout(); onLogout() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = CompassColors.Error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign out & lock")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── API KEY DIALOG ────────────────────────────────────────────
    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(apiKey ?: "") }
        var showKey  by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Anthropic API Key") },
            text = {
                Column {
                    Text("Enter your API key from console.anthropic.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("sk-ant-…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setApiKey(keyInput.trim())
                    showApiKeyDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = CompassColors.Blue400,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val modifier = if (onClick != null)
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    else
        Modifier.fillMaxWidth()

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
        else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AccountRow(email: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(40.dp), tint = CompassColors.Blue400)
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Google Account", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(email, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
}
