package com.compass.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.compass.diary.ui.navigation.CompassNavGraph
import com.compass.diary.ui.theme.CompassTheme
import com.compass.diary.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ splash screen API
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            // Observe dark mode preference
            val darkModePref by preferencesManager.darkMode.collectAsState(initial = "SYSTEM")
            val darkTheme = when (darkModePref) {
                "DARK"  -> true
                "LIGHT" -> false
                else    -> isSystemInDarkTheme()
            }

            CompassTheme(darkTheme = darkTheme) {
                CompassNavGraph(navController = navController)
            }
        }
    }

}
