package com.example.smartvocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.smartvocab.navigation.NavGraph
import com.example.smartvocab.ui.theme.SmartVocabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartVocabTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}