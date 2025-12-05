package com.example.tombyts_android

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("/")
    suspend fun getResponse(): Response<String>

    @POST("/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("/auth/validate")
    suspend fun validateToken(@Header("Authorization") token: String): Response<TokenValidationResponse>

    @GET("/movies")
    suspend fun getMovies(@Header("Authorization") token: String): Response<List<Movie>>

    @GET("/movies/{movieTitle}") // New route for fetching movie details
    suspend fun getMovieDetails(
        @Path("movieTitle") movieTitle: String,
        @Header("Authorization") token: String
    ): Response<MovieDetails>

    @POST("/progress/{movieId}")
    suspend fun updateProgress(
        @Path("movieId") movieId: String,
        @Header("Authorization") token: String,
        @Body progressUpdate: ProgressUpdate
    ): Response<ProgressResponse>

    @GET("/progress/{movieId}")
    suspend fun getProgress(
        @Path("movieId") movieId: String,
        @Header("Authorization") token: String
    ): Response<ProgressResponse>

    @GET("/subs/subtitles/{movieTitle}/{language}") // Added subtitle endpoint
    suspend fun getSubtitles(
        @Path("movieTitle") movieTitle: String,
        @Path("language") language: String,
        @Header("Authorization") token: String
    ): Response<String> // Assuming the response is the subtitle content as a string

    // Note: /movies endpoint returns all media (both movies and TV episodes)
    // We parse the path field to distinguish between them:
    // - "Movies/..." = movie
    // - "TV Shows/..." = TV episode
}
