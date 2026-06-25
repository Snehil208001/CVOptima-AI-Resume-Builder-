package com.snehil.cvoptima.data.remote

import com.snehil.cvoptima.data.local.dao.TokenDao
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class AuthInterceptor @Inject constructor(
    private val tokenDaoProvider: Provider<TokenDao>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Fetch the active token using blocking coroutines
        val token = runBlocking {
            tokenDaoProvider.get().getTokenFlow().firstOrNull()
        }
        
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
