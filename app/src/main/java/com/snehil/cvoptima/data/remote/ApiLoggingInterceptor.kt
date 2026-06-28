package com.snehil.cvoptima.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class ApiLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // 1. Log Request Info
        Log.i("API_MONITOR", "--> SENDING REQUEST: ${request.method} ${request.url}")
        
        // Log Request Headers
        val requestHeaders = request.headers
        for (i in 0 until requestHeaders.size) {
            Log.d("API_MONITOR", "    Header: ${requestHeaders.name(i)}: ${requestHeaders.value(i)}")
        }

        // Log Request Body
        val requestBody = request.body
        if (requestBody != null) {
            try {
                val buffer = okio.Buffer()
                requestBody.writeTo(buffer)
                val contentType = requestBody.contentType()
                val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                val bodyText = buffer.readString(charset)
                Log.d("API_MONITOR", "    Request Body: $bodyText")
            } catch (e: Exception) {
                Log.e("API_MONITOR", "    Failed to read request body: ${e.message}")
            }
        }

        // 2. Proceed with Request
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: IOException) {
            Log.e("API_MONITOR", "!!! REQUEST FAILED: ${request.method} ${request.url} - Error: ${e.message}", e)
            throw e
        }

        // 3. Log Response Info
        val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
        val responseCode = response.code
        val isSuccessful = response.isSuccessful

        if (isSuccessful) {
            Log.i("API_MONITOR", "<-- RECEIVED RESPONSE: $responseCode OK ${request.url} in ${duration}ms")
        } else {
            Log.e("API_MONITOR", "!!! ERROR RESPONSE: $responseCode ${response.message} for ${request.method} ${request.url} in ${duration}ms")
        }

        // Log Response Body
        val responseBody = response.body
        if (responseBody != null) {
            try {
                val contentType = responseBody.contentType()
                val bodyString = responseBody.string()
                Log.d("API_MONITOR", "    Response Body: $bodyString")
                
                // Reconstruct the response body since string() consumed it
                return response.newBuilder()
                    .body(bodyString.toResponseBody(contentType))
                    .build()
            } catch (e: Exception) {
                Log.e("API_MONITOR", "    Failed to read response body: ${e.message}")
            }
        }

        return response
    }
}
