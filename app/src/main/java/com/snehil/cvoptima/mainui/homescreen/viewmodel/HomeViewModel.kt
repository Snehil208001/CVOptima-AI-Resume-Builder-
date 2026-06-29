package com.snehil.cvoptima.mainui.homescreen.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.domain.model.Document
import com.snehil.cvoptima.domain.repository.DocumentRepository
import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.data.remote.model.*
import com.snehil.cvoptima.data.local.dao.*
import com.snehil.cvoptima.data.local.entity.*
import com.snehil.cvoptima.mainui.generator.ResumePdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import com.snehil.cvoptima.domain.repository.TokenRepository
import com.snehil.cvoptima.data.local.AppDatabase

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val documents: List<Document>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val apiService: ApiService,
    private val basicInfoDao: BasicInfoDao,
    private val educationDao: EducationDao,
    private val experienceDao: ExperienceDao,
    private val skillGroupDao: SkillGroupDao,
    private val projectDao: ProjectDao,
    private val certificationDao: CertificationDao,
    private val layoutSettingsDao: LayoutSettingsDao,
    private val tokenRepository: TokenRepository,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileDto?>(null)
    val userProfile: StateFlow<UserProfileDto?> = _userProfile.asStateFlow()

    init {
        loadDocuments()
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = apiService.getProfile()
                _userProfile.value = profile
            } catch (e: Exception) {
                android.util.Log.e("API_MONITOR", "Failed to load user profile", e)
                // Ignore and fall back to "Professional Builder"
            }
        }
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            documentRepository.getRecentDocuments()
                .catch { exception ->
                    _uiState.value = HomeUiState.Error(exception.message ?: "Failed to load documents")
                }
                .collect { documents ->
                    _uiState.value = HomeUiState.Success(documents)
                }
        }
    }

    fun tailorNewResume(jobTitle: String, companyName: String, jdText: String) {
        viewModelScope.launch {
            documentRepository.createDocument(
                jobTitle = jobTitle,
                companyName = companyName,
                jdText = jdText,
                resumeMd = "# $jobTitle at $companyName\n\nOptimized Resume content matching job description:\n$jdText"
            )
            loadDocuments()
        }
    }

    fun updateDocument(id: String, jobTitle: String, companyName: String, resumeMd: String) {
        viewModelScope.launch {
            documentRepository.updateDocument(id, jobTitle, companyName, resumeMd)
            loadDocuments()
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(id)
            loadDocuments()
        }
    }

    fun downloadTailoredPdf(context: Context, document: Document, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = ResumePdfExporter.exportMarkdownPdf(
                context,
                "${document.jobTitle}_at_${document.companyName}",
                document.resumeMd
            )
            onComplete(success)
        }
    }

    fun downloadMainPdf(context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Compile the UserProfileDto from local Room DB
                val basicInfo = basicInfoDao.getByResumeId(1L)
                val edus = educationDao.getByResumeId(1L)
                val exps = experienceDao.getByResumeId(1L)
                val sgs = skillGroupDao.getByResumeId(1L)
                val projs = projectDao.getByResumeId(1L)
                val certs = certificationDao.getByResumeId(1L)
                val layout = layoutSettingsDao.getByResumeId(1L)

                val profileDto = UserProfileDto(
                    username = _userProfile.value?.username ?: "default_user",
                    email = basicInfo?.email ?: (_userProfile.value?.email ?: ""),
                    name = basicInfo?.name ?: (_userProfile.value?.name ?: _userProfile.value?.username ?: "Compiled Resume"),
                    contactNumber = basicInfo?.contactNumber,
                    linkedinUrl = basicInfo?.linkedinUrl,
                    githubUrl = basicInfo?.githubUrl,
                    portfolioUrl = basicInfo?.portfolioUrl,
                    professionalSummary = basicInfo?.professionalSummary,
                    experiences = exps.map {
                        ExperienceDto(
                            id = it.id,
                            company = it.company,
                            title = it.title,
                            startDate = it.startDate,
                            endDate = it.endDate,
                            isCurrentRole = it.isCurrentRole,
                            description = it.description,
                            location = it.location,
                            type = it.type,
                            bulletPoints = it.bulletPoints
                        )
                    },
                    educations = edus.map {
                        EducationDto(
                            id = it.id,
                            institution = it.institution,
                            degree = it.degree,
                            fieldOfStudy = it.fieldOfStudy,
                            startDate = it.startDate,
                            endDate = it.endDate,
                            gpa = it.gpa,
                            score = it.score,
                            location = it.location
                        )
                    },
                    skillGroups = sgs.map {
                        SkillGroupDto(
                            id = it.id,
                            label = it.label,
                            skills = it.skills
                        )
                    },
                    projects = projs.map {
                        ProjectDto(
                            id = it.id,
                            title = it.title,
                            link = it.link,
                            date = it.date,
                            techStack = it.techStack,
                            bulletPoints = it.bulletPoints
                        )
                    },
                    certifications = certs.map {
                        CertificationDto(
                            id = it.id,
                            title = it.title,
                            issuer = it.issuer,
                            link = it.link,
                            date = it.date,
                            bulletPoints = it.bulletPoints
                        )
                    },
                    layoutConfig = LayoutConfigDto(
                        layoutDensity = layout?.layoutDensity ?: "Normal",
                        sectionOrder = layout?.sectionOrder ?: listOf("Skills", "Work Experiences", "Projects", "Certifications")
                    )
                )

                val success = ResumePdfExporter.exportPdf(context, profileDto)
                onComplete(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun clearResumeData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // Clear Room
                basicInfoDao.deleteByResumeId(1L)
                educationDao.deleteByResumeId(1L)
                experienceDao.deleteByResumeId(1L)
                skillGroupDao.deleteByResumeId(1L)
                projectDao.deleteByResumeId(1L)
                certificationDao.deleteByResumeId(1L)
                layoutSettingsDao.deleteByResumeId(1L)

                // Sync empty profile to remote
                val emptyProfile = UserProfileDto(
                    username = "default_user",
                    email = "",
                    name = "",
                    contactNumber = "",
                    linkedinUrl = "",
                    githubUrl = "",
                    portfolioUrl = "",
                    location = "",
                    professionalSummary = "",
                    experiences = emptyList(),
                    educations = emptyList(),
                    skillGroups = emptyList(),
                    projects = emptyList(),
                    certifications = emptyList(),
                    layoutConfig = null
                )
                apiService.syncProfile(emptyProfile)
                
                // Reload profile
                loadUserProfile()
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("RESET_PROFILE", "Failed to reset profile data", e)
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Wipe database cleanly under a single atomic transaction
                appDatabase.clearAllUserData()

                // Clear authentication token
                tokenRepository.clearToken()

                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("API_MONITOR", "Failed to logout", e)
            }
        }
    }

    suspend fun analyzeTextWithAi(resumeText: String): com.snehil.cvoptima.data.remote.model.AtsAnalysisResponse? {
        return try {
            android.util.Log.i("ATS_AI", "Sending PDF text (${resumeText.length} chars) to AI backend")
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                apiService.analyzeAts(
                    com.snehil.cvoptima.data.remote.model.AtsAnalysisRequest(
                        resumeText = resumeText,
                        targetJobDescription = null
                    )
                )
            }
            android.util.Log.i("ATS_AI", "AI PDF response (Full): $response")
            response
        } catch (e: Exception) {
            android.util.Log.e("ATS_AI", "AI PDF analysis failed", e)
            null
        }
    }
}
