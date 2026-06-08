package com.example.smartvocab.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartvocab.ui.onboarding.*
import com.example.smartvocab.ui.auth.*
import com.example.smartvocab.viewmodel.AuthViewModel
import com.example.smartvocab.ui.dashboard.*
import com.example.smartvocab.ui.vocabulary.*
import com.example.smartvocab.ui.learning.*
import com.example.smartvocab.ui.practice.*
import com.example.smartvocab.ui.progress.*
import com.example.smartvocab.ui.notification.*
import com.example.smartvocab.ui.settings.*

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
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
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController, authViewModel = authViewModel)
        }
        composable(Screen.OtpVerification.route) {
            OtpVerificationScreen(navController = navController, authViewModel = authViewModel)
        }

        // Main App Container (with Bottom Navigation)
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }

        // Vocabulary Module Nested Graph
        vocabularyGraph(navController = navController)

        // Flashcard Learning Screen
        composable(
            route = Screen.FlashcardLearning.route,
            arguments = listOf(
                navArgument("setId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                },
                navArgument("mode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                }
            )
        ) { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId").let { if (it == "null") null else it }
            val mode = backStackEntry.arguments?.getString("mode").let { if (it == "null") null else it }
            FlashcardScreen(navController = navController, setId = setId, mode = mode)
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
                navArgument("time") { type = NavType.StringType },
                navArgument("setId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val total = backStackEntry.arguments?.getInt("total") ?: 0
            val time = backStackEntry.arguments?.getString("time") ?: "00:00"
            val setId = backStackEntry.arguments?.getString("setId").let { if (it == "null") null else it }
            QuizResultScreen(navController = navController, score = score, total = total, time = time, setId = setId)
        }

        // Notification Screen
        composable(Screen.Notification.route) {
            NotificationScreen(navController = navController)
        }
    }
}
