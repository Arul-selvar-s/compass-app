package com.compass.diary.ui.screens.unlock

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.SetupViewModel
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat

@Composable
fun UnlockSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val step       by viewModel.step.collectAsState()
    val heading    by viewModel.heading.collectAsState()
    val savedAngle1 by viewModel.savedAngle1.collectAsState()
    val isBiometricAvailable by viewModel.isBiometricAvailable.collectAsState()

    val stepTexts = listOf(
        "Welcome to Compass" to "Your private diary, hidden in plain sight.",
        "Create Your Lock" to "Point your compass to your first secret direction.",
        "Confirm Direction 1" to "Saved! Now point to your second direction.",
        "Security Options" to "Choose additional security methods.",
        "All Set!" to "Your compass lock is ready."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Color(0xFF060C1A), Color(0xFF0D2047)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Step indicator
            StepIndicator(currentStep = step, totalSteps = 5)

            Spacer(Modifier.height(40.dp))

            // Title
            AnimatedContent(targetState = step, label = "title") { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stepTexts[s.coerceIn(0, 4)].first,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CompassColors.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stepTexts[s.coerceIn(0, 4)].second,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CompassColors.Silver400,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Step content
            when (step) {
                0 -> WelcomeContent()
                1 -> DirectionPicker(
                    heading = heading,
                    onConfirm = { viewModel.saveAngle1(heading) }
                )
                2 -> DirectionPicker(
                    heading = heading,
                    onConfirm = { viewModel.saveAngle2(heading) },
                    savedAngle = savedAngle1
                )
                3 -> SecurityOptions(
                    isBiometricAvailable = isBiometricAvailable,
                    onBiometricToggle = viewModel::setBiometric,
                    onContinue = viewModel::finishSecurity
                )
                4 -> SuccessContent(onContinue = {
                    viewModel.completeSetup()
                    onSetupComplete()
                })
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalSteps) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == currentStep) 24.dp else 8.dp, 8.dp)
                    .background(
                        if (i <= currentStep) CompassColors.Blue400 else CompassColors.Silver700,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun WelcomeContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🧭", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2047)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                FeatureRow("🔒", "Hidden behind a compass")
                FeatureRow("✨", "Unlimited daily pages")
                FeatureRow("🤖", "AI-powered search")
                FeatureRow("☁️", "Google Drive sync")
                FeatureRow("🎨", "Rich text & drawing")
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = CompassColors.Silver200)
    }
}

@Composable
private fun DirectionPicker(heading: Float, onConfirm: () -> Unit, savedAngle: Float? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Large heading display
        Text(
            text = "${heading.toInt()}°",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = CompassColors.Blue300
        )
        Text(
            text = headingToCardinal(heading),
            style = MaterialTheme.typography.titleLarge,
            color = CompassColors.Silver400
        )

        if (savedAngle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "First direction: ${savedAngle.toInt()}°",
                style = MaterialTheme.typography.bodySmall,
                color = CompassColors.Gold400
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CompassColors.Blue600)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Lock this direction")
        }
    }
}

@Composable
private fun SecurityOptions(
    isBiometricAvailable: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    var biometricEnabled by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isBiometricAvailable) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2047)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Biometric unlock", color = CompassColors.White, fontWeight = FontWeight.Medium)
                        Text("Fingerprint / Face ID", style = MaterialTheme.typography.bodySmall, color = CompassColors.Silver400)
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it; onBiometricToggle(it) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CompassColors.Blue600)
        ) { Text("Continue") }
    }
}

@Composable
private fun SuccessContent(onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✅", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Your compass lock is set!\nLong-press the compass center to unlock your diary.",
            style = MaterialTheme.typography.bodyLarge,
            color = CompassColors.Silver200,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CompassColors.Blue600)
        ) { Text("Open My Diary") }
    }
}

private fun headingToCardinal(h: Float) = when (h) {
    in 337.5f..360f, in 0f..22.5f -> "North"
    in 22.5f..67.5f  -> "North-East"
    in 67.5f..112.5f -> "East"
    in 112.5f..157.5f -> "South-East"
    in 157.5f..202.5f -> "South"
    in 202.5f..247.5f -> "South-West"
    in 247.5f..292.5f -> "West"
    in 292.5f..337.5f -> "North-West"
    else -> "North"
}
