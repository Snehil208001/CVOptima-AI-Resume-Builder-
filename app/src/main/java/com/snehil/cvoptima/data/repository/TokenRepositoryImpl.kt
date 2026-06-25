package com.snehil.cvoptima.data.repository

import com.snehil.cvoptima.data.local.dao.TokenDao
import com.snehil.cvoptima.data.local.entity.TokenEntity
import com.snehil.cvoptima.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TokenRepositoryImpl @Inject constructor(
    private val tokenDao: TokenDao
) : TokenRepository {

    override suspend fun saveToken(token: String) {
        tokenDao.saveToken(TokenEntity(token = token))
    }

    override fun getToken(): Flow<String?> {
        return tokenDao.getTokenFlow()
    }

    override suspend fun clearToken() {
        tokenDao.clearToken()
    }
}
