package com.snehil.cvoptima.data.remote

import com.snehil.cvoptima.data.local.dao.TokenDao
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class AuthInterceptor @Inject constructor(
    private val tokenDaoProvider: Provider<TokenDao>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Fetch the active token synchronously from Room
        val token = tokenDaoProvider.get().getTokenSync()
        
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
