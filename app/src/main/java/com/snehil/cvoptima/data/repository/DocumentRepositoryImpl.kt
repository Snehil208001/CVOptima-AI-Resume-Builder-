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
    private val mockDocuments = mutableListOf(
        Document(
            id = UUID.randomUUID().toString(),
            userId = 1L,
            jobTitle = "Senior Software Engineer",
            companyName = "Google",
            jdText = "Looking for a software engineer with 5+ years of experience in Kotlin and Android development.",
            resumeMd = "# John Doe\n## Senior Android Engineer\n5+ years of experience building modern Android applications...",
            status = "OPTIMIZED"
        ),
        Document(
            id = UUID.randomUUID().toString(),
            userId = 1L,
            jobTitle = "Product Manager",
            companyName = "Stripe",
            jdText = "Seeking a product manager to scale global payments infrastructure. Experience with APIs preferred.",
            resumeMd = "# Jane Smith\n## Technical Product Manager\nFocused on API product design, integration, and ecosystem expansion...",
            status = "GENERATED"
        ),
        Document(
            id = UUID.randomUUID().toString(),
            userId = 1L,
            jobTitle = "Data Scientist",
            companyName = "Netflix",
            jdText = "Build recommendation systems using deep learning, reinforcement learning, and big data pipeline tools.",
            resumeMd = "# Alex Lee\n## Data Scientist & ML Engineer\nDeveloped recommendation models increasing engagement by 15%...",
            status = "DRAFT"
        )
    )

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
}
