package com.snehil.cvoptima.mainui.generator.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.core.di.NetworkModule
import com.snehil.cvoptima.data.local.dao.*
import com.snehil.cvoptima.data.remote.model.*
import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.mainui.generator.ResumePdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject

@HiltViewModel
class StreamingGenerationViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val basicInfoDao: BasicInfoDao,
    private val educationDao: EducationDao,
    private val experienceDao: ExperienceDao,
    private val skillGroupDao: SkillGroupDao,
    private val projectDao: ProjectDao,
    private val certificationDao: CertificationDao,
    private val layoutSettingsDao: LayoutSettingsDao,
    private val apiService: ApiService
) : ViewModel() {

    private val _streamedText = MutableStateFlow("")
    val streamedText = _streamedText.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone = _isDone.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var eventSource: EventSource? = null

    fun startStreaming(taskId: String) {
        if (eventSource != null) return

        _streamedText.value = ""
        _isDone.value = false
        _error.value = null

        val request = Request.Builder()
            .url("${NetworkModule.BASE_URL}api/v1/ai/optimize/$taskId/stream")
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // Connection successfully opened
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                when (type) {
                    "token" -> {
                        _streamedText.value += data
                    }
                    "done" -> {
                        _isDone.value = true
                        eventSource.cancel()
                    }
                    "error" -> {
                        _error.value = data
                        eventSource.cancel()
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                _isDone.value = true
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!_isDone.value) {
                    _error.value = t?.message ?: "Connection connection to stream failed"
                }
            }
        }

        val sseFactory = EventSources.createFactory(okHttpClient)
        eventSource = sseFactory.newEventSource(request, listener)
    }

    fun downloadPdf(context: Context, onComplete: (Boolean) -> Unit) {
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

                val userProfile = try {
                    apiService.getProfile()
                } catch (e: Exception) {
                    null
                }

                val profileDto = UserProfileDto(
                    username = userProfile?.username ?: "default_user",
                    email = basicInfo?.email ?: (userProfile?.email ?: ""),
                    name = basicInfo?.name ?: (userProfile?.name ?: userProfile?.username ?: "Compiled Resume"),
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

    override fun onCleared() {
        super.onCleared()
        eventSource?.cancel()
    }
}
