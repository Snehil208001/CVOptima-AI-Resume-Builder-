package com.snehil.cvoptima.core.common

import com.google.gson.Gson
import retrofit2.HttpException

data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)

fun Throwable.getErrorMessage(): String {
    android.util.Log.e("API_MONITOR", "Error mapped in NetworkErrorMapper: ${this.message}", this)
    return when (this) {
        is HttpException -> {
            try {
                val errorBody = response()?.errorBody()?.string()
                android.util.Log.e("API_MONITOR", "HTTP Exception Error Body: $errorBody")
                val apiError = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                apiError.message ?: "An unexpected error occurred"
            } catch (e: Exception) {
                android.util.Log.e("API_MONITOR", "Failed to parse HTTP error body", e)
                "An unexpected network error occurred"
            }
        }
        else -> this.message ?: "An unknown error occurred"
    }
}
