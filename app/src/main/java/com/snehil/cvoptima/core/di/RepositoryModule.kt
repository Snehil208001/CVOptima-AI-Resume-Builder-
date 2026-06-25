package com.snehil.cvoptima.core.di

import com.snehil.cvoptima.data.repository.TokenRepositoryImpl
import com.snehil.cvoptima.domain.repository.TokenRepository
import com.snehil.cvoptima.data.repository.DocumentRepositoryImpl
import com.snehil.cvoptima.domain.repository.DocumentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTokenRepository(
        tokenRepositoryImpl: TokenRepositoryImpl
    ): TokenRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        documentRepositoryImpl: DocumentRepositoryImpl
    ): DocumentRepository
}
