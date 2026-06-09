package com.compass.diary.ui.screens.compass

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.CompassViewModel
import kotlinx.coroutines.delay
import kotlin.math.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat

@Composable
fun CompassScreen(
    onUnlocked: () -> Unit,
    viewModel: CompassViewModel = hiltViewModel()
) {
    val heading     by viewModel.heading.collectAsState()
    val unlockState by viewModel.unlockState.collectAsState()
    val unlockStep  by viewModel.unlockStep.collectAsState()

    // Animated needle rotation
    val needleRotation by animateFloatAsState(
        targetValue = -heading,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "needle"
    )

    // Navigate when unlocked
    LaunchedEffect(unlockState) {
        if (unlockState == CompassViewModel.UnlockState.UNLOCKED) {
            delay(600)
            onUnlocked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0D1B3E), Color(0xFF060C1A)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            // App name (small, subtle)
            Text(
                text = "COMPASS",
                style = MaterialTheme.typography.labelSmall,
                color = CompassColors.Silver400.copy(alpha = 0.5f),
                letterSpacing = 6.sp
            )

            Spacer(Modifier.height(32.dp))

            // ── COMPASS DIAL ──────────────────────────────────────
            CompassDial(
                heading = heading,
                needleRotation = needleRotation,
                unlockState = unlockState,
                unlockStep = unlockStep,
                onLongPress = { viewModel.startUnlock() },
                onTap = { viewModel.confirmStep() }
            )

            Spacer(Modifier.height(32.dp))

            // Heading readout
            HeadingDisplay(heading = heading)

            Spacer(Modifier.weight(1f))

            // Unlock instruction hint (only shown during unlock flow)
            AnimatedVisibility(
                visible = unlockState == CompassViewModel.UnlockState.STEP_1 ||
                          unlockState == CompassViewModel.UnlockState.STEP_2,
                enter = fadeIn() + slideInVertically { it },
                exit  = fadeOut()
            ) {
                UnlockInstructions(unlockStep = unlockStep)
            }

            Spacer(Modifier.height(48.dp))
        }

        // Success overlay
        AnimatedVisibility(
            visible = unlockState == CompassViewModel.UnlockState.UNLOCKED,
            enter = scaleIn() + fadeIn(),
            exit  = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(CompassColors.Blue500.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 48.sp, color = CompassColors.Blue400)
            }
        }
    }
}

@Composable
private fun CompassDial(
    heading: Float,
    needleRotation: Float,
    unlockState: CompassViewModel.UnlockState,
    unlockStep: Int,
    onLongPress: () -> Unit,
    onTap: () -> Unit
) {
    val isUnlocking = unlockState == CompassViewModel.UnlockState.STEP_1 ||
                      unlockState == CompassViewModel.UnlockState.STEP_2

    // Pulse animation when in unlock mode
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { if (isUnlocking) onTap() }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Outer ring
            drawCircle(
                color = Color(0xFF1E3A6E),
                radius = radius,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF0D2047),
                radius = radius - 4.dp.toPx()
            )

            // Inner decorative rings
            drawCircle(
                color = Color(0xFF1A3060).copy(alpha = 0.5f),
                radius = radius * 0.85f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF1A3060).copy(alpha = 0.3f),
                radius = radius * 0.6f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Cardinal direction ticks + labels
            drawCompassMarkings(center, radius)

            // Needle (rotated by heading)
            rotate(needleRotation, pivot = center) {
                drawNeedle(center, radius)
            }

            // Center circle
            drawCircle(color = Color(0xFF2563C8), radius = 12.dp.toPx(), center = center)
            drawCircle(color = Color(0xFF93C5FD), radius = 6.dp.toPx(), center = center)
        }

        // Unlock pulse ring
        if (isUnlocking) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = CompassColors.Blue400.copy(alpha = pulseAlpha * 0.3f),
                    radius = size.width / 2,
                    style = Stroke(width = 6.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawCompassMarkings(center: Offset, radius: Float) {
    val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
    val intercardinals = listOf("NE" to 45f, "SE" to 135f, "SW" to 225f, "NW" to 315f)

    // Major ticks every 10°, minor every 5°
    for (angle in 0 until 360 step 5) {
        val isMajor = angle % 10 == 0
        val tickLength = if (isMajor) radius * 0.08f else radius * 0.04f
        val rad = Math.toRadians(angle.toDouble())
        val startX = center.x + (radius * 0.88f) * sin(rad).toFloat()
        val startY = center.y - (radius * 0.88f) * cos(rad).toFloat()
        val endX   = center.x + (radius * 0.88f - tickLength) * sin(rad).toFloat()
        val endY   = center.y - (radius * 0.88f - tickLength) * cos(rad).toFloat()
        drawLine(
            color = if (isMajor) Color(0xFF4B82D0) else Color(0xFF2A4B7A),
            start = Offset(startX, startY),
            end   = Offset(endX, endY),
            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawNeedle(center: Offset, radius: Float) {
    val needleLength = radius * 0.72f
    val needleWidth  = 8.dp.toPx()

    // North needle (red)
    drawPath(
        path = needlePath(center, needleLength, needleWidth, true),
        color = Color(0xFFEF4444)
    )
    // South needle (white/silver)
    drawPath(
        path = needlePath(center, needleLength * 0.6f, needleWidth, false),
        color = Color(0xFFE5E7EB)
    )
}

private fun needlePath(center: Offset, length: Float, width: Float, north: Boolean): Path {
    val direction = if (north) -1f else 1f
    return Path().apply {
        moveTo(center.x, center.y + direction * width / 2)
        lineTo(center.x - width / 2, center.y)
        lineTo(center.x, center.y - direction * length)
        lineTo(center.x + width / 2, center.y)
        close()
    }
}

@Composable
private fun HeadingDisplay(heading: Float) {
    val cardinal = when (heading) {
        in 337.5f..360f, in 0f..22.5f -> "N"
        in 22.5f..67.5f  -> "NE"
        in 67.5f..112.5f -> "E"
        in 112.5f..157.5f -> "SE"
        in 157.5f..202.5f -> "S"
        in 202.5f..247.5f -> "SW"
        in 247.5f..292.5f -> "W"
        in 292.5f..337.5f -> "NW"
        else -> "N"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${heading.toInt()}°  $cardinal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Light,
            color = CompassColors.Blue300,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun UnlockInstructions(unlockStep: Int) {
    val text = when (unlockStep) {
        1 -> "Point to your first direction, then tap"
        2 -> "Point to your second direction, then tap"
        else -> ""
    }
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = CompassColors.Blue300,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
