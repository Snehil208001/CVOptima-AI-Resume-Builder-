package com.snehil.cvoptima.data.remote

import com.snehil.cvoptima.data.remote.model.AuthRequest
import com.snehil.cvoptima.data.remote.model.AuthResponse
import com.snehil.cvoptima.data.remote.model.RegisterRequest
import com.snehil.cvoptima.data.remote.model.DocumentDto
import com.snehil.cvoptima.data.remote.model.UserProfileDto
import com.snehil.cvoptima.data.remote.model.ExperienceDto
import com.snehil.cvoptima.data.remote.model.ExperienceOptimizationRequest
import com.snehil.cvoptima.data.remote.model.ExperienceOptimizationResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

interface ApiService {
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): AuthResponse

    @GET("api/v1/documents")
    suspend fun getDocuments(
        @Query("userId") userId: Long? = null
    ): List<DocumentDto>

    @GET("api/v1/documents/{id}")
    suspend fun getDocumentById(
        @Path("id") id: String
    ): DocumentDto

    @POST("api/v1/documents")
    suspend fun createDocument(
        @Body request: DocumentDto
    ): DocumentDto

    @GET("api/v1/profile")
    suspend fun getProfile(): UserProfileDto

    @PUT("api/v1/profile")
    suspend fun updateProfile(
        @Body request: UserProfileDto
    ): UserProfileDto

    @PATCH("api/v1/profile/experience")
    suspend fun saveExperience(
        @Body request: ExperienceDto
    ): ExperienceDto

    @DELETE("api/v1/profile/experience/{expId}")
    suspend fun deleteExperience(
        @Path("expId") expId: Long
    ): Response<Void>

    @POST("api/v1/ai/optimize")
    suspend fun optimizeExperience(
        @Body request: ExperienceOptimizationRequest
    ): ExperienceOptimizationResponse
}
