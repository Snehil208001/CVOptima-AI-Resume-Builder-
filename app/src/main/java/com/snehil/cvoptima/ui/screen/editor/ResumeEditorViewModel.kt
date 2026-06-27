package com.snehil.cvoptima.ui.screen.editor

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.data.local.dao.*
import com.snehil.cvoptima.data.local.entity.*
import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.data.remote.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResumeEditorViewModel @Inject constructor(
    private val basicInfoDao: BasicInfoDao,
    private val educationDao: EducationDao,
    private val experienceDao: ExperienceDao,
    private val skillGroupDao: SkillGroupDao,
    private val projectDao: ProjectDao,
    private val certificationDao: CertificationDao,
    private val layoutSettingsDao: LayoutSettingsDao,
    private val apiService: ApiService
) : ViewModel() {

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var contactNumber by mutableStateOf("")
    var linkedinUrl by mutableStateOf("")
    var githubUrl by mutableStateOf("")
    var portfolioUrl by mutableStateOf("")
    var location by mutableStateOf("")
    var yearsOfExp by mutableStateOf("")
    var specialization by mutableStateOf("")
    var targetRole by mutableStateOf("")
    var primaryTechnologies by mutableStateOf("")
    var autoGenerateSummary by mutableStateOf(false)
    var professionalSummary by mutableStateOf("")

    // Step 2: Lists
    val educations = mutableStateListOf<LocalEducation>()
    val projects = mutableStateListOf<LocalProject>()
    val experiences = mutableStateListOf<LocalExperience>()
    val certifications = mutableStateListOf<LocalCertification>()

    // Step 3: Skills
    val skillGroups = mutableStateListOf<LocalSkillGroup>()

    // Step 4: Layout Settings
    var layoutDensity by mutableStateOf("Normal") // "Compact", "Normal", "Spacious"
    val sectionOrder = mutableStateListOf("Skills", "Work Experiences", "Projects", "Certifications", "Education")

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            try {
                // Try pulling remote first to seed DB
                val profile = apiService.getProfile()
                syncRemoteToLocal(profile)
            } catch (e: Exception) {
                // Ignore API failure and fall back to local Room cache
            }
            loadFromRoom()
            _syncState.value = SyncState.Idle
        }
    }

    private suspend fun syncRemoteToLocal(profile: UserProfileDto) {
        // Save Basic Info
        basicInfoDao.deleteByResumeId(1L)
        basicInfoDao.insert(
            LocalBasicInfo(
                name = profile.name ?: "${profile.firstName ?: ""} ${profile.lastName ?: ""}".trim(),
                email = profile.email,
                contactNumber = profile.contactNumber,
                linkedinUrl = profile.linkedinUrl,
                githubUrl = profile.githubUrl,
                portfolioUrl = profile.portfolioUrl,
                location = profile.location,
                professionalSummary = profile.professionalSummary,
                resumeId = 1L
            )
        )

        // Save Educations
        educationDao.deleteByResumeId(1L)
        profile.educations?.let { list ->
            educationDao.insertAll(list.map {
                LocalEducation(
                    id = 0,
                    institution = it.institution,
                    degree = it.degree,
                    fieldOfStudy = it.fieldOfStudy,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    gpa = it.gpa,
                    score = it.score,
                    location = it.location,
                    resumeId = 1L
                )
            })
        }

        // Save Experiences
        experienceDao.deleteByResumeId(1L)
        profile.experiences?.let { list ->
            experienceDao.insertAll(list.map {
                LocalExperience(
                    id = 0,
                    company = it.company,
                    title = it.title,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    isCurrentRole = it.isCurrentRole,
                    description = it.description,
                    location = it.location,
                    type = it.type,
                    bulletPoints = it.bulletPoints ?: emptyList(),
                    resumeId = 1L
                )
            })
        }

        // Save Projects
        projectDao.deleteByResumeId(1L)
        profile.projects?.let { list ->
            projectDao.insertAll(list.map {
                LocalProject(
                    id = 0,
                    title = it.title,
                    link = it.link,
                    date = it.date,
                    techStack = it.techStack,
                    bulletPoints = it.bulletPoints ?: emptyList(),
                    resumeId = 1L
                )
            })
        }

        // Save Skill Groups
        skillGroupDao.deleteByResumeId(1L)
        profile.skillGroups?.let { list ->
            skillGroupDao.insertAll(list.map {
                LocalSkillGroup(
                    id = 0,
                    label = it.label,
                    skills = it.skills ?: emptyList(),
                    resumeId = 1L
                )
            })
        }

        // Save Certifications
        certificationDao.deleteByResumeId(1L)
        profile.certifications?.let { list ->
            certificationDao.insertAll(list.map {
                LocalCertification(
                    id = 0,
                    title = it.title,
                    issuer = it.issuer,
                    link = it.link,
                    date = it.date,
                    bulletPoints = it.bulletPoints ?: emptyList(),
                    resumeId = 1L
                )
            })
        }

        // Save Layout settings
        profile.layoutConfig?.let { config ->
            layoutSettingsDao.deleteByResumeId(1L)
            layoutSettingsDao.insert(
                LocalLayoutSettings(
                    id = 0,
                    layoutDensity = config.layoutDensity ?: "Normal",
                    sectionOrder = config.sectionOrder ?: listOf("Skills", "Work Experiences", "Projects", "Certifications"),
                    resumeId = 1L
                )
            )
        }
    }

    private suspend fun loadFromRoom() {
        val info = basicInfoDao.getByResumeId(1L)
        if (info != null) {
            name = info.name ?: ""
            email = info.email ?: ""
            contactNumber = info.contactNumber ?: ""
            linkedinUrl = info.linkedinUrl ?: ""
            githubUrl = info.githubUrl ?: ""
            portfolioUrl = info.portfolioUrl ?: ""
            location = info.location ?: ""
            professionalSummary = info.professionalSummary ?: ""
        }

        educations.clear()
        educations.addAll(educationDao.getByResumeId(1L))

        experiences.clear()
        experiences.addAll(experienceDao.getByResumeId(1L))

        projects.clear()
        projects.addAll(projectDao.getByResumeId(1L))

        skillGroups.clear()
        skillGroups.addAll(skillGroupDao.getByResumeId(1L))

        certifications.clear()
        certifications.addAll(certificationDao.getByResumeId(1L))

        val layout = layoutSettingsDao.getByResumeId(1L)
        if (layout != null) {
            layoutDensity = layout.layoutDensity
            if (layout.sectionOrder.isNotEmpty()) {
                sectionOrder.clear()
                sectionOrder.addAll(layout.sectionOrder)
            }
        }
    }

    fun generateAiSummary() {
        val summary = buildString {
            append("Results-driven professional ")
            if (yearsOfExp.isNotBlank()) {
                append("with $yearsOfExp of experience ")
            }
            if (specialization.isNotBlank()) {
                append("specializing in $specialization. ")
            } else {
                append("seeking to leverage technical capabilities. ")
            }
            if (targetRole.isNotBlank()) {
                append("Seeking a role as a $targetRole. ")
            }
            val tech = if (primaryTechnologies.isNotBlank()) {
                primaryTechnologies
            } else {
                skillGroups.flatMap { it.skills }.take(6).joinToString(", ")
            }
            if (tech.isNotEmpty()) {
                append("Proficient in $tech. ")
            }
            if (experiences.isNotEmpty()) {
                val topExp = experiences.first()
                append("Demonstrated success delivering high-quality results at ${topExp.company}.")
            }
        }
        professionalSummary = summary.trim()
    }

    fun validateInputs(): String? {
        if (name.isBlank()) return "Name cannot be empty"
        if (email.isBlank()) return "Email cannot be empty"
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email address format"
        if (contactNumber.isBlank()) return "Contact number cannot be empty"
        return null
    }

    fun saveAndSync(onComplete: (Boolean, String) -> Unit) {
        val validationError = validateInputs()
        if (validationError != null) {
            onComplete(false, validationError)
            return
        }

        _syncState.value = SyncState.Syncing

        viewModelScope.launch {
            try {
                // 1. Save locally to offline cache
                basicInfoDao.deleteByResumeId(1L)
                basicInfoDao.insert(
                    LocalBasicInfo(
                        name = name,
                        email = email,
                        contactNumber = contactNumber,
                        linkedinUrl = linkedinUrl,
                        githubUrl = githubUrl,
                        portfolioUrl = portfolioUrl,
                        location = location,
                        professionalSummary = professionalSummary,
                        resumeId = 1L
                    )
                )

                educationDao.deleteByResumeId(1L)
                educationDao.insertAll(educations.map { it.copy(id = 0) })

                experienceDao.deleteByResumeId(1L)
                experienceDao.insertAll(experiences.map { it.copy(id = 0) })

                projectDao.deleteByResumeId(1L)
                projectDao.insertAll(projects.map { it.copy(id = 0) })

                skillGroupDao.deleteByResumeId(1L)
                skillGroupDao.insertAll(skillGroups.map { it.copy(id = 0) })

                certificationDao.deleteByResumeId(1L)
                certificationDao.insertAll(certifications.map { it.copy(id = 0) })

                layoutSettingsDao.deleteByResumeId(1L)
                layoutSettingsDao.insert(
                    LocalLayoutSettings(
                        layoutDensity = layoutDensity,
                        sectionOrder = sectionOrder.toList(),
                        resumeId = 1L
                    )
                )

                // 2. Build DTO to send to server
                val profileDto = UserProfileDto(
                    username = email.substringBefore("@"),
                    email = email,
                    name = name,
                    contactNumber = contactNumber,
                    linkedinUrl = linkedinUrl,
                    githubUrl = githubUrl,
                    portfolioUrl = portfolioUrl,
                    location = location,
                    professionalSummary = professionalSummary,
                    experiences = experiences.map {
                        ExperienceDto(
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
                    educations = educations.map {
                        EducationDto(
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
                    skillGroups = skillGroups.map {
                        SkillGroupDto(
                            label = it.label,
                            skills = it.skills
                        )
                    },
                    projects = projects.map {
                        ProjectDto(
                            title = it.title,
                            link = it.link,
                            date = it.date,
                            techStack = it.techStack,
                            bulletPoints = it.bulletPoints
                        )
                    },
                    certifications = certifications.map {
                        CertificationDto(
                            title = it.title,
                            issuer = it.issuer,
                            link = it.link,
                            date = it.date,
                            bulletPoints = it.bulletPoints
                        )
                    },
                    layoutConfig = LayoutConfigDto(
                        layoutDensity = layoutDensity,
                        sectionOrder = sectionOrder.toList()
                    )
                )

                // 3. Make master sync call
                apiService.syncProfile(profileDto)

                _syncState.value = SyncState.Idle
                onComplete(true, "Profile synchronized successfully!")
            } catch (e: Exception) {
                _syncState.value = SyncState.Idle
                // Offline fallback - saved locally but remote sync failed
                onComplete(true, "Offline cache updated. Server sync failed: ${e.localizedMessage}")
            }
        }
    }
}

sealed interface SyncState {
    object Idle : SyncState
    object Loading : SyncState
    object Syncing : SyncState
}
