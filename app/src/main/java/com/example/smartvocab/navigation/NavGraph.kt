package com.example.smartvocab.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.smartvocab.ui.onboarding.*
import com.example.smartvocab.ui.auth.*
import com.example.smartvocab.ui.dashboard.*
import com.example.smartvocab.ui.vocabulary.*
import com.example.smartvocab.ui.learning.*
import com.example.smartvocab.ui.practice.*
import com.example.smartvocab.ui.progress.*
import com.example.smartvocab.ui.notification.*
import com.example.smartvocab.ui.settings.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        // Onboarding Screens
        composable(Screen.Welcome.route) {
            WelcomeScreen(navController = navController)
        }
        composable(Screen.Goal.route) {
            GoalScreen(navController = navController)
        }
        composable(Screen.Level.route) {
            LevelScreen(navController = navController)
        }

        // Auth Screens
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // Main App Container (with Bottom Navigation)
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }

        // Vocabulary Detail Screen
        composable(
            route = Screen.VocabSetDetail.route,
            arguments = listOf(navArgument("setId") { type = NavType.StringType })
        ) { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId") ?: ""
            VocabSetDetailScreen(navController = navController, setId = setId)
        }

        // Add/Edit Word Screen
        composable(
            route = Screen.AddEditWord.route,
            arguments = listOf(
                navArgument("setId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                },
                navArgument("wordId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                }
            )
        ) { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId").let { if (it == "null") null else it }
            val wordId = backStackEntry.arguments?.getString("wordId").let { if (it == "null") null else it }
            AddEditWordScreen(navController = navController, setId = setId, wordId = wordId)
        }

        // Flashcard Learning Screen
        composable(
            route = Screen.FlashcardLearning.route,
            arguments = listOf(
                navArgument("setId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                }
            )
        ) { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId").let { if (it == "null") null else it }
            FlashcardScreen(navController = navController, setId = setId)
        }

        // Quiz Screen
        composable(
            route = Screen.QuizQuestion.route,
            arguments = listOf(
                navArgument("setId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                }
            )
        ) { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId").let { if (it == "null") null else it }
            QuizScreen(navController = navController, setId = setId)
        }

        // Quiz Result Screen
        composable(
            route = Screen.QuizResult.route,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType },
                navArgument("time") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val total = backStackEntry.arguments?.getInt("total") ?: 0
            val time = backStackEntry.arguments?.getString("time") ?: "00:00"
            QuizResultScreen(navController = navController, score = score, total = total, time = time)
        }

        // Notification Screen
        composable(Screen.Notification.route) {
            NotificationScreen(navController = navController)
        }
    }
}
