package com.snehil.cvoptima.data.repository

import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.data.remote.model.DocumentDto
import com.snehil.cvoptima.domain.model.Document
import com.snehil.cvoptima.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DocumentRepository {

    // Keep an in-memory mutable list of documents so that "Tailor New Resume" additions persist during the app run
    private val mockDocuments = mutableListOf<Document>()

    override fun getRecentDocuments(): Flow<List<Document>> = flow {
        try {
            val response = apiService.getDocuments(userId = 1L)
            val docs = response.map { dto ->
                Document(
                    id = dto.id ?: UUID.randomUUID().toString(),
                    userId = dto.userId ?: 1L,
                    jobTitle = dto.jobTitle ?: "Untitled Position",
                    companyName = dto.companyName ?: "Unknown Company",
                    jdText = dto.jdText ?: "",
                    resumeMd = dto.resumeMd ?: "",
                    status = dto.status ?: "DRAFT"
                )
            }
            if (docs.isEmpty()) {
                emit(mockDocuments)
            } else {
                emit(docs)
            }
        } catch (e: Exception) {
            android.util.Log.e("API_MONITOR", "Failed to fetch documents from server", e)
            emit(mockDocuments)
        }
    }

    override suspend fun createDocument(
        jobTitle: String,
        companyName: String,
        jdText: String,
        resumeMd: String
    ): Document? {
        val newDocDto = DocumentDto(
            id = UUID.randomUUID().toString(),
            userId = 1L,
            jobTitle = jobTitle,
            companyName = companyName,
            jdText = jdText,
            resumeMd = resumeMd,
            status = "OPTIMIZED"
        )
        return try {
            val response = apiService.createDocument(newDocDto)
            val savedDoc = Document(
                id = response.id ?: newDocDto.id!!,
                userId = response.userId ?: 1L,
                jobTitle = response.jobTitle ?: jobTitle,
                companyName = response.companyName ?: companyName,
                jdText = response.jdText ?: jdText,
                resumeMd = response.resumeMd ?: resumeMd,
                status = response.status ?: "OPTIMIZED"
            )
            mockDocuments.add(0, savedDoc)
            savedDoc
        } catch (e: Exception) {
            android.util.Log.e("API_MONITOR", "Failed to create document on server, using local fallback", e)
            val fallbackDoc = Document(
                id = newDocDto.id!!,
                userId = 1L,
                jobTitle = jobTitle,
                companyName = companyName,
                jdText = jdText,
                resumeMd = resumeMd,
                status = "OPTIMIZED"
            )
            mockDocuments.add(0, fallbackDoc)
            fallbackDoc
        }
    }

    override suspend fun updateDocument(
        id: String,
        jobTitle: String,
        companyName: String,
        resumeMd: String
    ): Document? {
        val updateDto = DocumentDto(
            id = id,
            userId = 1L,
            jobTitle = jobTitle,
            companyName = companyName,
            jdText = null,
            resumeMd = resumeMd,
            status = "OPTIMIZED"
        )
        return try {
            val response = apiService.updateDocument(id, updateDto)
            val updatedDoc = Document(
                id = response.id ?: id,
                userId = response.userId ?: 1L,
                jobTitle = response.jobTitle ?: jobTitle,
                companyName = response.companyName ?: companyName,
                jdText = response.jdText ?: "",
                resumeMd = response.resumeMd ?: resumeMd,
                status = response.status ?: "OPTIMIZED"
            )
            val idx = mockDocuments.indexOfFirst { it.id == id }
            if (idx >= 0) {
                mockDocuments[idx] = updatedDoc
            }
            updatedDoc
        } catch (e: Exception) {
            android.util.Log.e("API_MONITOR", "Failed to update document on server, using local fallback", e)
            val updatedDoc = Document(
                id = id,
                userId = 1L,
                jobTitle = jobTitle,
                companyName = companyName,
                jdText = "",
                resumeMd = resumeMd,
                status = "OPTIMIZED"
            )
            val idx = mockDocuments.indexOfFirst { it.id == id }
            if (idx >= 0) {
                mockDocuments[idx] = updatedDoc
            }
            updatedDoc
        }
    }

    override suspend fun deleteDocument(id: String): Boolean {
        return try {
            val response = apiService.deleteDocument(id)
            if (response.isSuccessful) {
                mockDocuments.removeAll { it.id == id }
                true
            } else {
                android.util.Log.e("API_MONITOR", "Delete document API response unsuccessful: ${response.code()}")
                mockDocuments.removeAll { it.id == id }
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("API_MONITOR", "Failed to delete document from server, using local fallback", e)
            mockDocuments.removeAll { it.id == id }
            true
        }
    }
}
