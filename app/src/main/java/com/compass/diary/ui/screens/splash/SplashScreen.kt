package com.compass.diary.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.compass.diary.ui.theme.CompassColors
import com.compass.diary.viewmodel.SplashViewModel
import kotlinx.coroutines.delay
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat

@Composable
fun SplashScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToCompass: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isSetupComplete by viewModel.isSetupComplete.collectAsState(initial = false)

    // Needle spin-up animation
    val needleAnim = rememberInfiniteTransition(label = "needle_spin")
    val needleRotation by needleAnim.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "needleRot"
    )

    // Text fade-in
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        titleVisible = true
        delay(600)
        subtitleVisible = true
        delay(1800)
        if (isSetupComplete) onNavigateToCompass() else onNavigateToSetup()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Color(0xFF060C1A), Color(0xFF0D1B3E)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Compass icon (drawn with Text emoji for now; replace with vector asset)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Text(text = "🧭", fontSize = 80.sp, modifier = Modifier.rotate(needleRotation * 0.1f))
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(600)) + slideInVertically { it / 2 }
            ) {
                Text(
                    text = "COMPASS",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = CompassColors.White,
                    letterSpacing = 8.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(600))
            ) {
                Text(
                    text = "Your private journal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CompassColors.Silver400,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
