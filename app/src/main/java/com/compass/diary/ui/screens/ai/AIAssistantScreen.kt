package com.compass.diary.ui.screens.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import com.compass.diary.viewmodel.AIViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onBack: () -> Unit,
    onOpenPage: (String) -> Unit,
    viewModel: AIViewModel = hiltViewModel()
) {
    val messages    by viewModel.messages.collectAsState()
    val isThinking  by viewModel.isThinking.collectAsState()
    val apiKeySet   by viewModel.isApiKeyConfigured.collectAsState()
    var input       by remember { mutableStateOf("") }
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()

    // Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AI Assistant", fontWeight = FontWeight.Bold)
                            Text(
                                if (apiKeySet) "Ready" else "API key not set",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (apiKeySet) CompassColors.Success else CompassColors.Warning
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(Icons.Default.ClearAll, "Clear")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (!apiKeySet) {
                ApiKeyPrompt()
            }

            // Suggested questions
            if (messages.isEmpty()) {
                SuggestedQuestions(onSelect = { q ->
                    viewModel.ask(q)
                    scope.launch { if (messages.isNotEmpty()) listState.animateScrollToItem(0) }
                })
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, onOpenPage = onOpenPage)
                }
                if (isThinking) {
                    item { ThinkingIndicator() }
                }
            }

            HorizontalDivider()

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your diary…") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CompassColors.Blue500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val q = input.trim()
                        if (q.isNotBlank()) {
                            input = ""
                            viewModel.ask(q)
                        }
                    },
                    enabled = input.isNotBlank() && !isThinking,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = CompassColors.Blue600)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SuggestedQuestions(onSelect: (String) -> Unit) {
    val suggestions = listOf(
        "Summarise this week",
        "What have I written about work?",
        "Find mentions of travel",
        "What are my goals?",
        "Show my happiest days",
        "What did I write last month?"
    )
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Ask anything about your diary",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        suggestions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSelect(suggestion) },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MessageBubble(message: AIViewModel.Message, onOpenPage: (String) -> Unit) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(CompassColors.Blue600, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Text("✨", style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            Surface(
                color = if (isUser) CompassColors.Blue600 else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = if (isUser) 20.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 20.dp,
                    bottomStart = 20.dp, bottomEnd = 20.dp
                )
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp, 10.dp)
                )
            }

            // Source date chips
            if (message.sourceDates.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    message.sourceDates.take(3).forEach { dateKey ->
                        AssistChip(
                            onClick = { onOpenPage(dateKey) },
                            label = { Text(dateKey, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(CompassColors.Blue600, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text("✨", style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                ThinkingDot(delayMs = 0)
                Spacer(Modifier.width(4.dp))
                ThinkingDot(delayMs = 200)
                Spacer(Modifier.width(4.dp))
                ThinkingDot(delayMs = 400)
            }
        }
    }
}

@Composable
private fun ThinkingDot(delayMs: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_$delayMs")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, delayMillis = delayMs),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot_alpha_$delayMs"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(CompassColors.Blue400.copy(alpha = alpha), RoundedCornerShape(4.dp))
    )
}

@Composable
private fun ApiKeyPrompt() {
    Surface(
        color = CompassColors.Warning.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = CompassColors.Warning, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Add your Anthropic API key in Settings → AI Assistant to enable AI features.",
                style = MaterialTheme.typography.bodySmall,
                color = CompassColors.Warning
            )
        }
    }
}
