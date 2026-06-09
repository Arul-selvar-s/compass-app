package com.compass.diary.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.compass.diary.ui.theme.CompassColors
import com.google.gson.Gson

data class DrawPath(
    val points: List<Pair<Float, Float>>,
    val color: Long,            // Packed ARGB stored as signed Long
    val strokeWidth: Float,
    val toolType: String        // PENCIL|PEN|MARKER|ERASER
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvas(
    onSave: (String) -> Unit,
    onClose: () -> Unit
) {
    var paths     by remember { mutableStateOf(listOf<DrawPath>()) }
    var redoStack by remember { mutableStateOf(listOf<DrawPath>()) }
    var current   by remember { mutableStateOf(listOf<Offset>()) }

    var selectedTool    by remember { mutableStateOf("PEN") }
    var selectedColor   by remember { mutableStateOf(Color.Black) }
    var strokeWidth     by remember { mutableStateOf(4f) }
    var showColorPicker by remember { mutableStateOf(false) }

    val toolColor = if (selectedTool == "ERASER") Color.White else selectedColor
    val toolStroke = when (selectedTool) {
        "PENCIL" -> strokeWidth * 1f
        "PEN"    -> strokeWidth * 2f
        "MARKER" -> strokeWidth * 6f
        "ERASER" -> strokeWidth * 10f
        else     -> strokeWidth * 2f
    }
    val toolAlpha = when (selectedTool) {
        "MARKER" -> 0.4f
        else     -> 1.0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── TOP BAR ───────────────────────────────────────────────
        Surface(shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                Text("Drawing", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    if (paths.isNotEmpty()) {
                        redoStack = redoStack + paths.last()
                        paths = paths.dropLast(1)
                    }
                }, enabled = paths.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                }
                IconButton(onClick = {
                    if (redoStack.isNotEmpty()) {
                        paths = paths + redoStack.last()
                        redoStack = redoStack.dropLast(1)
                    }
                }, enabled = redoStack.isNotEmpty()) {
                    Icon(Icons.Default.Redo, "Redo")
                }
                IconButton(onClick = { paths = emptyList(); redoStack = emptyList() }) {
                    Icon(Icons.Default.Delete, "Clear")
                }
                FilledTonalButton(
                    onClick = {
                        val json = Gson().toJson(paths.map {
                            mapOf(
                                "points" to it.points,
                                "color" to it.color,
                                "strokeWidth" to it.strokeWidth,
                                "toolType" to it.toolType
                            )
                        })
                        onSave(json)
                    }
                ) { Text("Insert") }
            }
        }

        // ── CANVAS ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(selectedTool, selectedColor, strokeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            current = listOf(offset)
                            redoStack = emptyList()
                        },
                        onDrag = { change, _ ->
                            current = current + change.position
                        },
                        onDragEnd = {
                            if (current.isNotEmpty()) {
                                paths = paths + DrawPath(
                                    points      = current.map { it.x to it.y },
                                    color       = toolColor.copy(alpha = toolAlpha).value.toLong(),
                                    strokeWidth = toolStroke,
                                    toolType    = selectedTool
                                )
                                current = emptyList()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw committed paths
                paths.forEach { path -> drawPath(path) }
                // Draw in-progress stroke
                if (current.size >= 2) {
                    val path = Path()
                    path.moveTo(current[0].x, current[0].y)
                    for (i in 1 until current.size) {
                        path.lineTo(current[i].x, current[i].y)
                    }
                    drawPath(
                        path  = path,
                        color = toolColor.copy(alpha = toolAlpha),
                        style = Stroke(
                            width = toolStroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // ── TOOL BAR ──────────────────────────────────────────────
        Surface(shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Tool selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolButton("✏️", "PENCIL", selectedTool) { selectedTool = "PENCIL"; strokeWidth = 2f }
                    ToolButton("🖊️", "PEN",    selectedTool) { selectedTool = "PEN";    strokeWidth = 3f }
                    ToolButton("🖌️", "MARKER", selectedTool) { selectedTool = "MARKER"; strokeWidth = 5f }
                    ToolButton("⬜", "ERASER", selectedTool) { selectedTool = "ERASER"; strokeWidth = 8f }

                    // Color swatch
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(selectedColor, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showColorPicker = !showColorPicker }
                    )
                }

                // Stroke size
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Size", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 1f..20f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Color palette
                if (showColorPicker) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val palette = listOf(
                            Color.Black, Color.White, Color.Red, Color(0xFFFF6B35),
                            Color(0xFFFFC300), Color(0xFF4CAF50), Color(0xFF2196F3),
                            Color(0xFF9C27B0), Color(0xFF795548), Color(0xFF607D8B)
                        )
                        palette.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        if (selectedColor == color) 2.dp else 0.dp,
                                        CompassColors.Blue400, CircleShape
                                    )
                                    .clickable { selectedColor = color; showColorPicker = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPath(drawPath: DrawPath) {
    if (drawPath.points.size < 2) return
    val path = Path()
    path.moveTo(drawPath.points[0].first, drawPath.points[0].second)
    for (i in 1 until drawPath.points.size) {
        path.lineTo(drawPath.points[i].first, drawPath.points[i].second)
    }
    drawPath(
        path  = path,
        color = Color(drawPath.color.toULong()),
        style = Stroke(
            width = drawPath.strokeWidth,
            cap   = StrokeCap.Round,
            join  = StrokeJoin.Round
        )
    )
}

@Composable
private fun ToolButton(emoji: String, tool: String, selected: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected == tool) CompassColors.Blue600 else Color.Transparent,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.titleMedium) }
    }
}
