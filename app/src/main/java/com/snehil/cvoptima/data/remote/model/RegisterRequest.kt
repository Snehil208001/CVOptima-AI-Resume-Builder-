package com.snehil.cvoptima.data.remote.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String?,
    val lastName: String?
)
