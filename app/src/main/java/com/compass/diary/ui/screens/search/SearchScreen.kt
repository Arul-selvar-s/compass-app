package com.compass.diary.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.data.local.entity.DiaryEntryEntity
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenPage: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val query   by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::search,
                        placeholder = { Text("Search your diary…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CompassColors.Blue500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text("Search across all pages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("words, sentences, tags, dates", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No results for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "${results.size} result${if (results.size != 1) "s" else ""} for \"$query\"",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(results, key = { it.dateKey }) { entry ->
                    SearchResultCard(entry = entry, query = query, onClick = { onOpenPage(entry.dateKey) })
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(entry: DiaryEntryEntity, query: String, onClick: () -> Unit) {
    // Find the first occurrence of the query in the text for the preview
    val idx = entry.plainText.indexOf(query, ignoreCase = true)
    val preview = if (idx >= 0) {
        val start = (idx - 40).coerceAtLeast(0)
        val end   = (idx + 80).coerceAtMost(entry.plainText.length)
        (if (start > 0) "…" else "") + entry.plainText.substring(start, end) + (if (end < entry.plainText.length) "…" else "")
    } else entry.plainText.take(100)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${entry.wordCount} words",
                style = MaterialTheme.typography.labelSmall,
                color = CompassColors.Blue400
            )
        }
    }
}
