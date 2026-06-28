package com.snehil.cvoptima.domain.repository

import com.snehil.cvoptima.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getRecentDocuments(): Flow<List<Document>>
    suspend fun createDocument(jobTitle: String, companyName: String, jdText: String, resumeMd: String): Document?
    suspend fun updateDocument(id: String, jobTitle: String, companyName: String, resumeMd: String): Document?
    suspend fun deleteDocument(id: String): Boolean
}
