package com.example.tombyts_android

import SimpleAuthPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.tombyts_android.Classes
import com.example.tombyts_android.Movie


@Composable
fun MovieListScreen(token: String, navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Movieslist")
            var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                try {
                    Log.d("MovieList", "Fetching movies with token: ${token.take(20)}...")
                    val response = Classes.ApiProvider.apiService.getMovies("Bearer $token")
                    if (response.isSuccessful) {
                        movies = response.body() ?: emptyList()
                        Log.d("MovieList", "Successfully fetched ${movies.size} movies")
                    } else {
                        val errorCode = response.code()
                        val errorMessage = response.message()
                        error = "Failed to fetch movies: $errorCode"

                        if (errorCode == 401) {
                            Log.e("Auth", "401 UNAUTHORIZED - Token expired or invalid. Token used: ${token.take(20)}...")
                            Log.e("Auth", "Full error: $errorCode $errorMessage")
                            val authPreferences = SimpleAuthPreferences(navController.context)
                            authPreferences.clearAuthData()
                            Thread.sleep(3000)
                            navController.navigate("login")
                        } else {
                            Log.e("API Error", "Failed to fetch movies: $errorCode $errorMessage")
                        }
                    }
                } catch (e: Exception) {
                    error = "Error: ${e.message}"
                    Log.e("MovieList", "Exception while fetching movies", e)
                } finally {
                    isLoading = false
                }
            }
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text("Error: $error")
            } else {
                LazyColumn {
                    items(movies) { movie ->
                        TextButton(onClick = {
                            val encodedTitle = Uri.encode(movie.title)
                            navController.navigate("moviePlayer/${encodedTitle}/${token}")
                        }) {
                            Text(text = movie.title)
                        }
                    }
                }
            }
        }
    }
}