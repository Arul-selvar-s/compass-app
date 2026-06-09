package com.compass.diary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.compass.diary.ui.screens.ai.AIAssistantScreen
import com.compass.diary.ui.screens.calendar.CalendarScreen
import com.compass.diary.ui.screens.compass.CompassScreen
import com.compass.diary.ui.screens.editor.DailyPageScreen
import com.compass.diary.ui.screens.home.DiaryHomeScreen
import com.compass.diary.ui.screens.reminders.RemindersScreen
import com.compass.diary.ui.screens.search.SearchScreen
import com.compass.diary.ui.screens.settings.SettingsScreen
import com.compass.diary.ui.screens.splash.SplashScreen
import com.compass.diary.ui.screens.starred.StarredScreen
import com.compass.diary.ui.screens.unlock.UnlockSetupScreen

object Routes {
    const val SPLASH         = "splash"
    const val SETUP_UNLOCK   = "setup_unlock"
    const val COMPASS        = "compass"
    const val HOME           = "home"
    const val DAILY_PAGE     = "daily_page/{dateKey}"
    const val CALENDAR       = "calendar"
    const val STARRED        = "starred"
    const val SEARCH         = "search"
    const val AI_ASSISTANT   = "ai_assistant"
    const val REMINDERS      = "reminders"
    const val SETTINGS       = "settings"

    fun dailyPage(dateKey: String) = "daily_page/$dateKey"
}

@Composable
fun CompassNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToSetup   = { navController.navigate(Routes.SETUP_UNLOCK) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToCompass = { navController.navigate(Routes.COMPASS) { popUpTo(Routes.SPLASH) { inclusive = true } } }
            )
        }

        composable(Routes.SETUP_UNLOCK) {
            UnlockSetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.COMPASS) {
                        popUpTo(Routes.SETUP_UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.COMPASS) {
            CompassScreen(
                onUnlocked = { navController.navigate(Routes.HOME) }
            )
        }

        composable(Routes.HOME) {
            DiaryHomeScreen(
                onOpenPage      = { dateKey -> navController.navigate(Routes.dailyPage(dateKey)) },
                onOpenCalendar  = { navController.navigate(Routes.CALENDAR) },
                onOpenStarred   = { navController.navigate(Routes.STARRED) },
                onOpenSearch    = { navController.navigate(Routes.SEARCH) },
                onOpenAI        = { navController.navigate(Routes.AI_ASSISTANT) },
                onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                onOpenSettings  = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.DAILY_PAGE,
            arguments = listOf(navArgument("dateKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateKey = backStackEntry.arguments?.getString("dateKey") ?: ""
            DailyPageScreen(
                dateKey = dateKey,
                onBack = { navController.popBackStack() },
                onOpenAI = { navController.navigate(Routes.AI_ASSISTANT) }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                onOpenPage = { dateKey -> navController.navigate(Routes.dailyPage(dateKey)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STARRED) {
            StarredScreen(
                onOpenPage = { dateKey -> navController.navigate(Routes.dailyPage(dateKey)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onOpenPage = { dateKey -> navController.navigate(Routes.dailyPage(dateKey)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AI_ASSISTANT) {
            AIAssistantScreen(
                onBack = { navController.popBackStack() },
                onOpenPage = { dateKey -> navController.navigate(Routes.dailyPage(dateKey)) }
            )
        }

        composable(Routes.REMINDERS) {
            RemindersScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.COMPASS) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

// NOTE: SharedPagesScreen is accessible from DiaryHomeScreen settings menu.
// Add to NavGraph by importing SharedPagesScreen and adding:
//
// composable(Routes.SHARED_PAGES) {
//     SharedPagesScreen(
//         onOpenPage = { dateKey -> navController.navigate(Routes.dailyPage(dateKey)) },
//         onBack = { navController.popBackStack() }
//     )
// }
//
// And add to Routes: const val SHARED_PAGES = "shared_pages"
