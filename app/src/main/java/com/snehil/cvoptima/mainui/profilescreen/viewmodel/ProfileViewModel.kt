package com.snehil.cvoptima.mainui.profilescreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.data.local.dao.EducationDao
import com.snehil.cvoptima.data.local.dao.ExperienceDao
import com.snehil.cvoptima.data.local.dao.SkillDao
import com.snehil.cvoptima.data.local.entity.LocalEducation
import com.snehil.cvoptima.data.local.entity.LocalExperience
import com.snehil.cvoptima.data.local.entity.LocalSkill
import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.data.remote.model.ExperienceDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.snehil.cvoptima.data.remote.model.UserProfileDto
import com.snehil.cvoptima.domain.repository.TokenRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val experienceDao: ExperienceDao,
    private val educationDao: EducationDao,
    private val skillDao: SkillDao,
    private val tokenRepository: TokenRepository,
    private val apiService: ApiService
) : ViewModel() {

    // Expose Room flows directly for tab views
    val experienceList: Flow<List<LocalExperience>> = experienceDao.getByResumeIdFlow(1L)
    val educationList: Flow<List<LocalEducation>> = educationDao.getByResumeIdFlow(1L)
    val skillList: Flow<List<LocalSkill>> = skillDao.getByResumeIdFlow(1L)

    private val _userProfile = MutableStateFlow<UserProfileDto?>(null)
    val userProfile: StateFlow<UserProfileDto?> = _userProfile.asStateFlow()

    init {
        pullProfileFromBackend()
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // Clear local Room database caches
                experienceDao.deleteByResumeId(1L)
                educationDao.deleteByResumeId(1L)
                skillDao.deleteByResumeId(1L)
                
                // Clear authentication token
                tokenRepository.clearToken()
                
                onSuccess()
            } catch (e: Exception) {
                // Handle logout exception
            }
        }
    }

    // Seeding/Syncing database from backend profile details on startup
    fun pullProfileFromBackend() {
        viewModelScope.launch {
            try {
                val profile = apiService.getProfile()
                _userProfile.value = profile

                // Sync experiences
                profile.experiences?.let { expDtos ->
                    experienceDao.deleteByResumeId(1L)
                    val expEntities = expDtos.map { dto ->
                        LocalExperience(
                            id = dto.id ?: 0L,
                            company = dto.company,
                            title = dto.title,
                            startDate = dto.startDate,
                            endDate = dto.endDate,
                            isCurrentRole = dto.isCurrentRole,
                            description = dto.description,
                            location = dto.location,
                            type = dto.type,
                            bulletPoints = dto.bulletPoints ?: emptyList(),
                            resumeId = 1L
                        )
                    }
                    experienceDao.insertAll(expEntities)
                }

                // Sync educations
                profile.educations?.let { eduDtos ->
                    educationDao.deleteByResumeId(1L)
                    val eduEntities = eduDtos.map { dto ->
                        LocalEducation(
                            id = dto.id ?: 0L,
                            institution = dto.institution,
                            degree = dto.degree,
                            fieldOfStudy = dto.fieldOfStudy,
                            startDate = dto.startDate,
                            endDate = dto.endDate,
                            gpa = dto.gpa,
                            score = dto.score,
                            location = dto.location,
                            resumeId = 1L
                        )
                    }
                    educationDao.insertAll(eduEntities)
                }

                // Sync skills
                profile.skillGroups?.let { groups ->
                    skillDao.deleteByResumeId(1L)
                    val skEntities = groups.flatMap { g ->
                        g.skills?.map { name ->
                            LocalSkill(
                                id = 0L,
                                name = name,
                                proficiencyLevel = null,
                                resumeId = 1L
                            )
                        } ?: emptyList()
                    }
                    skillDao.insertAll(skEntities)
                }
            } catch (e: Exception) {
                // Network pull failed; fall back cleanly to existing local Room cache
            }
        }
    }

    // Add or update single job history record and sync with backend
    fun saveExperience(
        id: Long,
        company: String,
        title: String,
        startDate: String?,
        endDate: String?,
        isCurrentRole: Boolean,
        description: String?
    ) {
        viewModelScope.launch {
            val localEntity = LocalExperience(
                id = id,
                company = company,
                title = title,
                startDate = startDate,
                endDate = endDate,
                isCurrentRole = isCurrentRole,
                description = description,
                location = null,
                type = null,
                bulletPoints = emptyList(),
                resumeId = 1L
            )

            val savedId = if (id == 0L) {
                experienceDao.insert(localEntity)
            } else {
                experienceDao.update(localEntity)
                id
            }

            // Sync single update to backend
            try {
                val syncDto = ExperienceDto(
                    id = if (savedId > 0) savedId else null,
                    company = company,
                    title = title,
                    startDate = startDate,
                    endDate = endDate,
                    isCurrentRole = isCurrentRole,
                    description = description,
                    location = null,
                    type = null,
                    bulletPoints = null
                )
                apiService.saveExperience(syncDto)
            } catch (e: Exception) {
                // Sync failed, fallback to local Room data stream
            }
        }
    }

    // Delete job history record and sync with backend
    fun deleteExperience(experience: LocalExperience) {
        viewModelScope.launch {
            experienceDao.delete(experience)
            try {
                apiService.deleteExperience(experience.id)
            } catch (e: Exception) {
                // Sync delete failed, local Room is source of truth
            }
        }
    }
}
