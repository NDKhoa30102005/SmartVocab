package com.example.smartvocab.ui.vocabulary

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.smartvocab.navigation.Screen

fun NavGraphBuilder.vocabularyGraph(navController: NavHostController) {
    // Vocabulary Set Detail Screen
    composable(
        route = Screen.VocabSetDetail.route,
        arguments = listOf(navArgument("setId") { type = NavType.StringType })
    ) { backStackEntry ->
        val setId = backStackEntry.arguments?.getString("setId") ?: ""
        VocabSetDetailScreen(navController = navController, setId = setId)
    }

    // Add/Edit Set Screen
    composable(
        route = Screen.AddEditSet.route,
        arguments = listOf(
            navArgument("setId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = "null"
            }
        )
    ) { backStackEntry ->
        val setId = backStackEntry.arguments?.getString("setId").let { if (it == "null") null else it }
        AddEditSetScreen(navController = navController, setId = setId)
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
}
