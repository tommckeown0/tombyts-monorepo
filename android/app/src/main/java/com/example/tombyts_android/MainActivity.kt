package com.example.tombyts_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import com.example.tombyts.ui.theme.TombytsTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TombytsTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()
                val navigateTo = intent.getStringExtra("navigate_to")

                NavHost(navController = navController, startDestination = navigateTo ?: "login") {
                    composable("login") {
                        LoginScreen(navController, snackbarHostState)
                    }
                    composable(
                        "movieList/{token}",
                        arguments = listOf(navArgument("token") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val token = backStackEntry.arguments?.getString("token") ?: ""
                        LaunchedEffect(Unit) {
                            val intent = Intent(this@MainActivity, MovieListActivity::class.java).apply {
                                putExtra("token", token)
                            }
                            startActivity(intent)
                        }
                    }
                    composable(
                        "moviePlayer/{movieTitle}/{token}",
                        arguments = listOf(
                            navArgument("movieTitle") { type = NavType.StringType },
                            navArgument("token") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
                        val token = backStackEntry.arguments?.getString("token") ?: ""

                        MoviePlayerScreen(movieTitle, token, navController)
                    }
                }
            }
        }
    }
}

