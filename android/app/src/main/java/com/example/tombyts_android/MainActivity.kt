package com.example.tombyts_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.tombyts.ui.theme.TombytsTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {

    // Create the shared ViewModel at the Activity level
    private val movieNavigationViewModel: MovieNavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TombytsTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()
                val navigateTo = intent.getStringExtra("navigate_to")
                val cameFromLeanback = intent.getBooleanExtra("came_from_leanback", false)

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
                                // Pass the ViewModel instance to the MovieListActivity
                                MovieListActivity.setSharedViewModel(movieNavigationViewModel)
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

                        if (cameFromLeanback) {
                            MoviePlayerScreenWithLeanbackBack(movieTitle, token, movieNavigationViewModel)
                        } else {
                            MoviePlayerScreen(movieTitle, token, navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoviePlayerScreenWithLeanbackBack(
    movieTitle: String,
    token: String,
    viewModel: MovieNavigationViewModel
) {
    val context = LocalContext.current

    // Handle back button to return to leanback UI
    BackHandler {
        // Mark that we're returning from player and set the movie to highlight
        viewModel.setReturningFromPlayer(true)
        viewModel.setSelectedMovie(movieTitle)

        val intent = Intent(context, MovieListActivity::class.java).apply {
            putExtra("token", token)
            // Pass the ViewModel instance to the MovieListActivity
            MovieListActivity.setSharedViewModel(viewModel)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        (context as Activity).finish()
    }

    // Use the regular MoviePlayerScreen but without navController navigation
    MoviePlayerScreen(movieTitle, token, rememberNavController())
}