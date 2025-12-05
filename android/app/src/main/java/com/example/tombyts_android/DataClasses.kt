package com.example.tombyts_android

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val message: String, val token: String?)

data class TokenValidationResponse(val valid: Boolean, val user: UserInfo)

data class UserInfo(val userId: String, val username: String)

data class Movie(val id: String?, val title: String, val path: String)

data class MovieDetails(
    val _id: String,
    val title: String,
    val path: String,
    val __v: Int
)

// TV Show data models for new file structure
data class TVShow(
    val id: String,
    val name: String,
    val seasonCount: Int,
    val path: String,
    var seasons: List<Season> = emptyList()
)

data class Season(
    val seasonNumber: Int,
    val showName: String,
    val episodeCount: Int,
    val path: String,
    var episodes: List<Episode> = emptyList()
)

data class Episode(
    val id: String?,
    val title: String,
    val showName: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val path: String
)