package com.example.tombyts_android

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val message: String, val token: String?)

data class TokenValidationResponse(val valid: Boolean, val user: UserInfo)

data class UserInfo(val userId: String, val username: String)

data class Movie(val id: String, val title: String, val path: String)

data class MovieDetails(
    val _id: String,
    val title: String,
    val path: String,
    val __v: Int
)