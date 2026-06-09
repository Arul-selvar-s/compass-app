package com.compass.diary.ui.screens.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compass.diary.ui.theme.CompassColors

// ─────────────────────────────────────────────────────────────────
// SHARED PAGES SCREEN
// Manages collaborative page sharing with 4 permission levels.
// ─────────────────────────────────────────────────────────────────

data class SharedPageInfo(
    val shareCode: String,
    val dateKey: String,
    val permission: SharePermission,
    val sharedWith: String?,
    val isActive: Boolean
)

enum class SharePermission(val label: String, val description: String) {
    READ_ONLY    ("Read only",        "Can read the page"),
    ADD_ONLY     ("Add content",      "Can add, but not edit existing content"),
    READ_WRITE   ("Read & write",     "Can read and edit, but not delete your content"),
    FULL         ("Full collaboration","Can read, edit, and delete own additions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPagesScreen(
    onOpenPage: (String) -> Unit,
    onBack: () -> Unit
) {
    // In a real app these would come from a ViewModel backed by SharedPageDao
    val sharedPages = remember {
        mutableStateListOf<SharedPageInfo>()
    }
    var showCreateSheet by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, null,
                            tint = CompassColors.Blue400,
                            modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Shared Pages", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = CompassColors.Blue600
            ) {
                Icon(Icons.Default.Add, "Share a page",
                    tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->

        if (sharedPages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔗", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text("No shared pages yet",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Share individual diary pages with others\nusing a secure link code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    // Permission level explainer
                    PermissionExplainerCard()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "${sharedPages.size} active share${if (sharedPages.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(sharedPages, key = { it.shareCode }) { page ->
                    SharedPageCard(
                        page    = page,
                        onOpen  = { onOpenPage(page.dateKey) },
                        onCopy  = {
                            clipboard.setText(AnnotatedString(page.shareCode))
                        },
                        onRevoke = { sharedPages.remove(page) }
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateShareSheet(
            onCreate = { dateKey, permission ->
                val code = generateShareCode()
                sharedPages.add(
                    SharedPageInfo(
                        shareCode  = code,
                        dateKey    = dateKey,
                        permission = permission,
                        sharedWith = null,
                        isActive   = true
                    )
                )
                showCreateSheet = false
            },
            onDismiss = { showCreateSheet = false }
        )
    }
}

@Composable
private fun PermissionExplainerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Permission levels",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = CompassColors.Blue400)
            Spacer(Modifier.height(10.dp))
            SharePermission.entries.forEach { perm ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    PermissionIcon(perm)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(perm.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium)
                        Text(perm.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedPageCard(
    page: SharedPageInfo,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onRevoke: () -> Unit
) {
    var showCode by remember { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PermissionIcon(page.permission)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(page.dateKey,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium)
                    Text(page.permission.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showCode = !showCode }) {
                    Icon(
                        if (showCode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Show code",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, "Copy code", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onRevoke) {
                    Icon(Icons.Default.LinkOff, "Revoke",
                        tint = CompassColors.Error,
                        modifier = Modifier.size(20.dp))
                }
            }

            AnimatedVisibility(visible = showCode) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        page.shareCode,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CompassColors.Blue400,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        letterSpacing = androidx.compose.ui.unit.TextUnit(2f,
                            androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionIcon(perm: SharePermission) {
    val (icon, tint) = when (perm) {
        SharePermission.READ_ONLY  -> Icons.Default.Visibility to MaterialTheme.colorScheme.onSurfaceVariant
        SharePermission.ADD_ONLY   -> Icons.Default.AddCircleOutline to CompassColors.Blue400
        SharePermission.READ_WRITE -> Icons.Default.EditNote to CompassColors.Gold400
        SharePermission.FULL       -> Icons.Default.Group to CompassColors.Success
    }
    Icon(icon, perm.label, tint = tint, modifier = Modifier.size(22.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateShareSheet(
    onCreate: (dateKey: String, permission: SharePermission) -> Unit,
    onDismiss: () -> Unit
) {
    var dateKey    by remember { mutableStateOf("") }
    var permission by remember { mutableStateOf(SharePermission.READ_ONLY) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Share a Page",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = dateKey,
                onValueChange = { dateKey = it },
                label = { Text("Date (e.g. 2026-06-07)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            Text("Permission level",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            SharePermission.entries.forEach { perm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = permission == perm,
                        onClick  = { permission = perm }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(perm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text(perm.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (dateKey.isNotBlank()) onCreate(dateKey.trim(), permission) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = dateKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CompassColors.Blue600)
            ) { Text("Create Share Link") }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun generateShareCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..8).map { chars.random() }.chunked(4).joinToString("-")
}
