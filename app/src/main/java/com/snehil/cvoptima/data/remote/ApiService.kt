package com.snehil.cvoptima.data.remote

import com.snehil.cvoptima.data.remote.model.AuthRequest
import com.snehil.cvoptima.data.remote.model.AuthResponse
import com.snehil.cvoptima.data.remote.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): AuthResponse
}
