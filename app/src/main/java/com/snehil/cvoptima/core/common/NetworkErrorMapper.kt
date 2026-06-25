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
    return when (this) {
        is HttpException -> {
            try {
                val errorBody = response()?.errorBody()?.string()
                val apiError = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                apiError.message ?: "An unexpected error occurred"
            } catch (e: Exception) {
                "An unexpected network error occurred"
            }
        }
        else -> this.message ?: "An unknown error occurred"
    }
}
