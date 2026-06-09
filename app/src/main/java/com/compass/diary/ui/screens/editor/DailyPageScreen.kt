package com.compass.diary.ui.screens.editor

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.ui.components.DrawingCanvas
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.util.SaveState
import com.compass.diary.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPageScreen(
    dateKey: String,
    onBack: () -> Unit,
    onOpenAI: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val entry     by viewModel.currentEntry.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val wordCount by viewModel.currentWordCount.collectAsState()

    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    var showDrawing by remember { mutableStateOf(false) }
    var showFormatBar by remember { mutableStateOf(true) }
    var boldActive by remember { mutableStateOf(false) }
    var italicActive by remember { mutableStateOf(false) }
    var underlineActive by remember { mutableStateOf(false) }
    var strikeActive by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Load entry content when available
    LaunchedEffect(entry) {
        entry?.let { e ->
            if (textValue.text.isEmpty() && e.plainText.isNotEmpty()) {
                textValue = TextFieldValue(e.plainText)
            }
        }
    }

    // Select current date's entry
    LaunchedEffect(dateKey) { viewModel.selectEntry(dateKey) }

    // Save on every text change (debounced inside ViewModel)
    LaunchedEffect(textValue.text) {
        if (textValue.text.isNotEmpty()) {
            viewModel.onContentChanged(dateKey, textValue.text)
        }
    }

    if (showDrawing) {
        DrawingCanvas(
            onSave  = { paths ->
                viewModel.saveDrawing(dateKey, paths)
                showDrawing = false
            },
            onClose = { showDrawing = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.forceSave(dateKey, textValue.text)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            entry?.title ?: dateKey,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$wordCount words  •  ${saveStateLabel(saveState)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = when (saveState) {
                                SaveState.SAVING -> CompassColors.Gold400
                                SaveState.SAVED  -> CompassColors.Success
                                SaveState.ERROR  -> CompassColors.Error
                                else             -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showVersionHistory = true }) {
                        Icon(Icons.Default.History, "Version History")
                    }
                    IconButton(onClick = onOpenAI) {
                        Icon(Icons.Default.AutoAwesome, "AI Assistant")
                    }
                    IconButton(onClick = {
                        val starred = textValue.selection
                        if (!starred.collapsed) {
                            val selectedText = textValue.text.substring(starred.start, starred.end)
                            viewModel.starContent(dateKey, selectedText)
                        }
                    }) {
                        Icon(Icons.Default.StarBorder, "Star selection")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── FORMAT TOOLBAR ─────────────────────────────────────
            AnimatedVisibility(visible = showFormatBar) {
                FormatToolbar(
                    boldActive      = boldActive,
                    italicActive    = italicActive,
                    underlineActive = underlineActive,
                    strikeActive    = strikeActive,
                    onBold          = { boldActive = !boldActive },
                    onItalic        = { italicActive = !italicActive },
                    onUnderline     = { underlineActive = !underlineActive },
                    onStrike        = { strikeActive = !strikeActive },
                    onInsertChecklist = {
                        textValue = textValue.insertText("\n☐ ")
                        viewModel.onContentChanged(dateKey, textValue.text)
                    },
                    onInsertBullet = {
                        textValue = textValue.insertText("\n• ")
                        viewModel.onContentChanged(dateKey, textValue.text)
                    },
                    onInsertNumbered = {
                        val lines = textValue.text.count { it == '\n' } + 1
                        textValue = textValue.insertText("\n$lines. ")
                        viewModel.onContentChanged(dateKey, textValue.text)
                    },
                    onDraw = { showDrawing = true }
                )
            }

            HorizontalDivider()

            // ── MAIN EDITOR ────────────────────────────────────────
            val scrollState = rememberScrollState()
            BasicRichTextField(
                value          = textValue,
                onValueChange  = {
                    textValue = it
                    viewModel.onContentChanged(dateKey, it.text)
                },
                boldActive     = boldActive,
                italicActive   = italicActive,
                underlineActive = underlineActive,
                modifier       = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }

    // Version History Bottom Sheet
    if (showVersionHistory) {
        VersionHistorySheet(
            dateKey = dateKey,
            viewModel = viewModel,
            onRestore = { version ->
                textValue = TextFieldValue(version)
                showVersionHistory = false
            },
            onDismiss = { showVersionHistory = false }
        )
    }
}

@Composable
private fun FormatToolbar(
    boldActive: Boolean, italicActive: Boolean,
    underlineActive: Boolean, strikeActive: Boolean,
    onBold: () -> Unit, onItalic: () -> Unit,
    onUnderline: () -> Unit, onStrike: () -> Unit,
    onInsertChecklist: () -> Unit, onInsertBullet: () -> Unit,
    onInsertNumbered: () -> Unit, onDraw: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FormatButton("B", boldActive, onBold, fontWeight = FontWeight.Bold)
        FormatButton("I", italicActive, onItalic, fontStyle = FontStyle.Italic)
        FormatButton("U", underlineActive, onUnderline, textDecoration = TextDecoration.Underline)
        FormatButton("S̶", strikeActive, onStrike)
        VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))
        FormatIconButton(Icons.Default.CheckBox, "Checklist", onInsertChecklist)
        FormatIconButton(Icons.Default.List, "Bullet", onInsertBullet)
        FormatIconButton(Icons.Default.FormatListNumbered, "Numbered", onInsertNumbered)
        VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))
        FormatIconButton(Icons.Default.Draw, "Draw", onDraw)
    }
}

@Composable
private fun FormatButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    textDecoration: TextDecoration? = null
) {
    Surface(
        onClick = onClick,
        color = if (active) CompassColors.Blue600 else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                style = MaterialTheme.typography.labelLarge.copy(textDecoration = textDecoration),
                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FormatIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, desc, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BasicRichTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    boldActive: Boolean,
    italicActive: Boolean,
    underlineActive: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            lineHeight = 26.sp
        ),
        decorationBox = { innerTextField ->
            if (value.text.isEmpty()) {
                Text(
                    "Write anything…",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
            innerTextField()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionHistorySheet(
    dateKey: String,
    viewModel: DiaryViewModel,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val history by viewModel.getVersionHistory(dateKey).collectAsState(initial = emptyList())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Version History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (history.isEmpty()) {
                Text("No versions saved yet.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                history.take(20).forEach { version ->
                    ListItem(
                        headlineContent = {
                            Text(java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(version.savedAt)))
                        },
                        supportingContent = { Text("${version.wordCount} words") },
                        trailingContent = {
                            TextButton(onClick = { onRestore(version.plainText) }) { Text("Restore") }
                        }
                    )
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun saveStateLabel(state: SaveState) = when (state) {
    SaveState.SAVING -> "Saving…"
    SaveState.SAVED  -> "Saved"
    SaveState.ERROR  -> "Save error"
    SaveState.IDLE   -> "Auto-save on"
}

private fun TextFieldValue.insertText(insert: String): TextFieldValue {
    val newText = text.substring(0, selection.end) + insert + text.substring(selection.end)
    val newCursor = selection.end + insert.length
    return copy(text = newText, selection = TextRange(newCursor))
}
