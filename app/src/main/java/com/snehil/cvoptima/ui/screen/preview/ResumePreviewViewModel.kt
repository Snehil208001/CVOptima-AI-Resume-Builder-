package com.snehil.cvoptima.ui.screen.preview

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.data.local.DatabaseSeeder
import com.snehil.cvoptima.data.local.dao.*
import com.snehil.cvoptima.data.local.entity.*
import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.data.remote.model.*
import com.snehil.cvoptima.mainui.generator.ResumePdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ResumePreviewViewModel @Inject constructor(
    private val basicInfoDao: BasicInfoDao,
    private val educationDao: EducationDao,
    private val experienceDao: ExperienceDao,
    private val skillGroupDao: SkillGroupDao,
    private val projectDao: ProjectDao,
    private val certificationDao: CertificationDao,
    private val layoutSettingsDao: LayoutSettingsDao,
    private val apiService: ApiService
) : ViewModel() {

    // Active Profile Data State
    var basicInfo by mutableStateOf<LocalBasicInfo?>(null)
    val educations = mutableStateListOf<LocalEducation>()
    val experiences = mutableStateListOf<LocalExperience>()
    val skillGroups = mutableStateListOf<LocalSkillGroup>()
    val projects = mutableStateListOf<LocalProject>()
    val certifications = mutableStateListOf<LocalCertification>()
    var layoutDensity by mutableStateOf("Normal")
    val sectionOrder = mutableStateListOf<String>()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting = _exporting.asStateFlow()

    // ATS Compatibility Report State
    var atsScore by mutableStateOf(70)
    var atsRating by mutableStateOf("Good Match")
    val atsLayoutTips = mutableStateListOf<String>()
    val atsKeywordTips = mutableStateListOf<String>()
    val atsMetricTips = mutableStateListOf<String>()
    val atsVerbTips = mutableStateListOf<String>()
    var isAiPowered by mutableStateOf(false)

    private val _aiAnalysisLoading = MutableStateFlow(false)
    val aiAnalysisLoading = _aiAnalysisLoading.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _loading.value = true
            try {
                DatabaseSeeder.seedIfNeeded(
                    basicInfoDao, educationDao, experienceDao,
                    skillGroupDao, projectDao, certificationDao, layoutSettingsDao
                )
                val info = basicInfoDao.getByResumeId(1L)
                basicInfo = info

                educations.clear()
                educations.addAll(educationDao.getByResumeId(1L))

                experiences.clear()
                experiences.addAll(experienceDao.getByResumeId(1L))

                skillGroups.clear()
                skillGroups.addAll(skillGroupDao.getByResumeId(1L))

                projects.clear()
                projects.addAll(projectDao.getByResumeId(1L))

                certifications.clear()
                certifications.addAll(certificationDao.getByResumeId(1L))

                val layout = layoutSettingsDao.getByResumeId(1L)
                if (layout != null) {
                    layoutDensity = layout.layoutDensity
                    sectionOrder.clear()
                    sectionOrder.addAll(layout.sectionOrder)
                } else {
                    layoutDensity = "Normal"
                    sectionOrder.clear()
                    sectionOrder.addAll(listOf("Skills", "Work Experiences", "Projects", "Certifications", "Education"))
                }

                // Compute ATS Score and suggestions
                calculateAtsReport()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    private fun calculateAtsReport() {
        var score = 0
        
        atsLayoutTips.clear()
        atsKeywordTips.clear()
        atsMetricTips.clear()
        atsVerbTips.clear()

        // 1. Header Evaluation (Max 20 pts)
        val info = basicInfo
        if (info != null) {
            var headerPts = 0
            if (!info.name.isNullOrBlank()) headerPts += 4
            if (!info.email.isNullOrBlank()) headerPts += 4
            if (!info.contactNumber.isNullOrBlank()) headerPts += 4
            if (!info.linkedinUrl.isNullOrBlank()) headerPts += 4
            if (!info.githubUrl.isNullOrBlank()) headerPts += 4
            score += headerPts

            if (info.location.isNullOrBlank()) {
                atsLayoutTips.add("Location is missing. Add your target city and country (e.g., Pune, India) to the header.")
            }
            if (info.linkedinUrl.isNullOrBlank()) {
                atsLayoutTips.add("LinkedIn link is missing. Add your LinkedIn profile URL for professional validation.")
            }
            if (info.githubUrl.isNullOrBlank()) {
                atsLayoutTips.add("GitHub link is missing. Add your GitHub profile URL to showcase coding contributions.")
            }
        } else {
            atsLayoutTips.add("Create your basic contact details in the profile builder.")
        }
        atsLayoutTips.add("Great: Your single-column layout is easily readable by parser systems.")

        // 2. Professional Summary Evaluation (Max 15 pts)
        if (info != null && !info.professionalSummary.isNullOrBlank()) {
            val summary = info.professionalSummary
            score += 5 // presence

            // Length Check (ideal 100-350 chars for 3-4 lines)
            if (summary.length in 100..350) {
                score += 5
            } else {
                atsKeywordTips.add("Keep professional summary between 3–4 lines (approx. 100–350 characters) to optimize readability.")
            }

            // Keyword content check
            val lowerSummary = summary.lowercase()
            if (lowerSummary.contains("years") || lowerSummary.contains("exp") || lowerSummary.contains("specializ") || lowerSummary.contains("developer") || lowerSummary.contains("engineer")) {
                score += 5
            } else {
                atsKeywordTips.add("Mention your years of experience, core specialization, and target job title in the summary.")
            }
        } else {
            atsKeywordTips.add("Add a Professional Summary to introduce your specialization and target role.")
        }

        // 3. Technical Skills Directory Evaluation (Max 15 pts)
        if (skillGroups.isNotEmpty()) {
            score += 5 // presence
            if (skillGroups.size >= 3) score += 5
            val totalSkills = skillGroups.flatMap { it.skills }.size
            if (totalSkills >= 10) {
                score += 5
            } else {
                atsKeywordTips.add("Add more key technology tags. An ideal tech directory contains 10+ matching keyword tags.")
            }
        } else {
            atsKeywordTips.add("Add technical skill groups (e.g. Languages, Databases, Android) to match job requirements.")
        }

        // 4. Work History Bullet Points Evaluation (Max 20 pts)
        if (experiences.isNotEmpty()) {
            score += 5 // presence
            
            var hasNumbers = false
            var hasActionVerbs = false
            var correctBulletCounts = true

            val actionVerbsList = listOf(
                "architected", "engineered", "implemented", "developed", "built", "optimized",
                "designed", "created", "led", "managed", "deployed", "orchestrated", "restructured",
                "integrated", "automated", "facilitated", "streamlined", "accelerated", "reduced"
            )

            for (exp in experiences) {
                val bullets = exp.bulletPoints ?: emptyList()
                if (bullets.size !in 4..6) {
                    correctBulletCounts = false
                }

                for (bullet in bullets) {
                    val lowerBullet = bullet.lowercase()
                    // Check for numbers, percentage, or currency symbols representing impact
                    if (lowerBullet.contains(Regex("\\d+%|\\b\\d+\\s*(hours|hrs|users|percent|projects|multiplier|x)\\b|\\$\\d+|million|thousand"))) {
                        hasNumbers = true
                    }
                    // Check for action verbs
                    if (actionVerbsList.any { verb -> lowerBullet.startsWith(verb) || lowerBullet.contains(" $verb") }) {
                        hasActionVerbs = true
                    }
                }
            }

            if (hasNumbers) {
                score += 5
            } else {
                atsMetricTips.add("Quantify your achievements in work experience. Include numbers (e.g., 'optimized performance by 30%', 'saved 2 hours/day').")
            }

            if (hasActionVerbs) {
                score += 5
            } else {
                atsVerbTips.add("Start experience bullet points with strong power/action verbs (e.g., 'Architected', 'Orchestrated', 'Implemented').")
            }

            if (correctBulletCounts) {
                score += 5
            } else {
                atsLayoutTips.add("Ensure each professional experience role contains exactly 4–6 impact-driven bullet points.")
            }

        } else {
            atsMetricTips.add("Add work experience entries representing achievements, metrics, and deliverables.")
        }

        // 5. Projects Evaluation (Max 15 pts)
        if (projects.isNotEmpty()) {
            score += 5 // presence
            var hasTechStack = true
            var correctProjBullets = true
            
            for (p in projects) {
                if (p.techStack.isNullOrBlank()) hasTechStack = false
                val bullets = p.bulletPoints ?: emptyList()
                if (bullets.size !in 3..5) {
                    correctProjBullets = false
                }
            }

            if (hasTechStack) score += 5
            else atsKeywordTips.add("Specify a comma-separated tech stack for every project.")

            if (correctProjBullets) score += 5
            else atsLayoutTips.add("Ensure projects have 3–5 concise, descriptive bullet points.")

        } else {
            atsLayoutTips.add("Add at least one key project featuring your core skills and tech stack.")
        }

        // 6. Credentials & Education Evaluation (Max 15 pts)
        if (educations.isNotEmpty()) {
            score += 5 // presence
            val topEdu = educations.first()
            if (!topEdu.degree.isNullOrBlank() && (!topEdu.score.isNullOrBlank() || topEdu.gpa != null)) {
                score += 5
            } else {
                atsLayoutTips.add("Specify your degree and CGPA/GPA in the education section.")
            }
        } else {
            atsLayoutTips.add("Add your college degree details under education.")
        }

        if (certifications.isNotEmpty()) {
            score += 5
        } else {
            atsLayoutTips.add("Include relevant certifications or hackathon achievements to validate credentials.")
        }

        // 7. Enforce Content Quality Constraints (Core ATS logic)
        var totalWorkBullets = 0
        for (exp in experiences) {
            totalWorkBullets += exp.bulletPoints?.size ?: 0
        }
        val totalSkillsCount = skillGroups.flatMap { it.skills }.size

        var finalScore = score.coerceIn(40, 99)
        
        // Critical Caps: An empty or sparse profile cannot bypass ATS filters
        if (experiences.isEmpty() || totalWorkBullets == 0) {
            finalScore = finalScore.coerceAtMost(48) // Cap under 50% if no job descriptions/bullets exist
            atsLayoutTips.add("❌ Critical: Add impact-driven bullet points to your work experiences. Parsers need text to scan.")
        } else if (totalWorkBullets < 4) {
            finalScore = finalScore.coerceAtMost(58) // Cap under 60% if details are too thin
            atsLayoutTips.add("⚠️ Work experience details are thin. Expand roles with more accomplishment bullet points.")
        }
        
        if (totalSkillsCount < 5) {
            finalScore = finalScore.coerceAtMost(62) // Cap if skill directory has under 5 items
            atsKeywordTips.add("❌ Critical: Add at least 5-10 core skills matching your target job keywords.")
        }

        if (projects.isEmpty()) {
            finalScore = finalScore.coerceAtMost(68) // Cap if no projects are listed to validate skills
        }

        atsScore = finalScore
        
        atsRating = when {
            atsScore >= 85 -> "Highly Compatible (Excellent)"
            atsScore >= 70 -> "ATS-Friendly (Good)"
            else -> "Needs Optimization"
        }
    }

    /**
     * Sends the user's profile data to the backend for AI-powered ATS analysis.
     * Falls back to local heuristic if backend is unreachable.
     */
    fun analyzeProfileWithAi() {
        viewModelScope.launch {
            _aiAnalysisLoading.value = true
            try {
                val resumeText = buildResumeText()
                Log.i("ATS_AI", "Sending resume text (${resumeText.length} chars) to AI backend")
                
                val response = withContext(Dispatchers.IO) {
                    apiService.analyzeAts(
                        AtsAnalysisRequest(
                            resumeText = resumeText,
                            targetJobDescription = null
                        )
                    )
                }
                
                Log.i("ATS_AI", "AI response: score=${response.score}, rating=${response.rating}")
                applyAiResponse(response)
                isAiPowered = true
            } catch (e: Exception) {
                Log.e("ATS_AI", "AI analysis failed, falling back to local heuristic", e)
                // Fallback to local calculation
                calculateAtsReport()
                isAiPowered = false
            } finally {
                _aiAnalysisLoading.value = false
            }
        }
    }

    /**
     * Sends arbitrary resume text (e.g. from an uploaded PDF) to the backend for AI analysis.
     * Returns true if AI was used, false if it fell back to local.
     */
    suspend fun analyzeTextWithAi(resumeText: String): AtsAnalysisResponse? {
        return try {
            Log.i("ATS_AI", "Sending PDF text (${resumeText.length} chars) to AI backend")
            val response = withContext(Dispatchers.IO) {
                apiService.analyzeAts(
                    AtsAnalysisRequest(
                        resumeText = resumeText,
                        targetJobDescription = null
                    )
                )
            }
            Log.i("ATS_AI", "AI PDF response (Full): $response")
            response
        } catch (e: Exception) {
            Log.e("ATS_AI", "AI PDF analysis failed", e)
            null
        }
    }

    private fun applyAiResponse(response: AtsAnalysisResponse) {
        atsScore = response.score
        atsRating = response.rating

        atsLayoutTips.clear()
        atsKeywordTips.clear()
        atsMetricTips.clear()
        atsVerbTips.clear()

        response.layoutSuggestions?.let { atsLayoutTips.addAll(it) }
        response.keywordSuggestions?.let { atsKeywordTips.addAll(it) }
        response.metricSuggestions?.let { atsMetricTips.addAll(it) }
        response.verbSuggestions?.let { atsVerbTips.addAll(it) }
    }

    /**
     * Serializes the user's profile data into a readable text format for AI analysis.
     */
    private fun buildResumeText(): String {
        val sb = StringBuilder()
        val info = basicInfo

        // Header
        if (info != null) {
            if (!info.name.isNullOrBlank()) sb.appendLine(info.name)
            val contactParts = listOfNotNull(
                info.email, info.contactNumber, info.location,
                info.linkedinUrl?.let { "LinkedIn: $it" },
                info.githubUrl?.let { "GitHub: $it" },
                info.portfolioUrl?.let { "Portfolio: $it" }
            )
            if (contactParts.isNotEmpty()) sb.appendLine(contactParts.joinToString(" | "))
            sb.appendLine()

            // Professional Summary
            if (!info.professionalSummary.isNullOrBlank()) {
                sb.appendLine("PROFESSIONAL SUMMARY")
                sb.appendLine(info.professionalSummary)
                sb.appendLine()
            }
        }

        // Skills
        if (skillGroups.isNotEmpty()) {
            sb.appendLine("TECHNICAL SKILLS")
            for (group in skillGroups) {
                sb.appendLine("${group.label}: ${group.skills.joinToString(", ")}")
            }
            sb.appendLine()
        }

        // Experience
        if (experiences.isNotEmpty()) {
            sb.appendLine("PROFESSIONAL EXPERIENCE")
            for (exp in experiences) {
                sb.appendLine("${exp.company} — ${exp.title}")
                val dates = listOfNotNull(exp.startDate, exp.endDate).joinToString(" – ")
                if (dates.isNotBlank()) sb.appendLine(dates)
                exp.bulletPoints?.forEach { bullet ->
                    sb.appendLine("• $bullet")
                }
                sb.appendLine()
            }
        }

        // Projects
        if (projects.isNotEmpty()) {
            sb.appendLine("PROJECTS")
            for (proj in projects) {
                sb.appendLine(proj.title)
                if (!proj.techStack.isNullOrBlank()) sb.appendLine("Tech: ${proj.techStack}")
                proj.bulletPoints?.forEach { bullet ->
                    sb.appendLine("• $bullet")
                }
                sb.appendLine()
            }
        }

        // Education
        if (educations.isNotEmpty()) {
            sb.appendLine("EDUCATION")
            for (edu in educations) {
                sb.appendLine("${edu.institution} — ${edu.degree}")
                if (!edu.fieldOfStudy.isNullOrBlank()) sb.append(" in ${edu.fieldOfStudy}")
                val dates = listOfNotNull(edu.startDate, edu.endDate).joinToString(" – ")
                if (dates.isNotBlank()) sb.appendLine(dates)
                val score = edu.score ?: edu.gpa?.toString()
                if (!score.isNullOrBlank()) sb.appendLine("CGPA: $score")
                sb.appendLine()
            }
        }

        // Certifications
        if (certifications.isNotEmpty()) {
            sb.appendLine("CERTIFICATIONS")
            for (cert in certifications) {
                sb.append("${cert.title}")
                if (!cert.issuer.isNullOrBlank()) sb.append(" — ${cert.issuer}")
                sb.appendLine()
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    fun exportPdf(context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _exporting.value = true
            try {
                val basicInfo = basicInfo
                val profileDto = UserProfileDto(
                    username = basicInfo?.email?.substringBefore("@") ?: "user",
                    email = basicInfo?.email ?: "",
                    name = basicInfo?.name ?: "",
                    contactNumber = basicInfo?.contactNumber,
                    linkedinUrl = basicInfo?.linkedinUrl,
                    githubUrl = basicInfo?.githubUrl,
                    portfolioUrl = basicInfo?.portfolioUrl,
                    location = basicInfo?.location,
                    professionalSummary = basicInfo?.professionalSummary,
                    experiences = experiences.map {
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
                    educations = educations.map {
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
                    skillGroups = skillGroups.map {
                        SkillGroupDto(
                            id = it.id,
                            label = it.label,
                            skills = it.skills
                        )
                    },
                    projects = projects.map {
                        ProjectDto(
                            id = it.id,
                            title = it.title,
                            link = it.link,
                            date = it.date,
                            techStack = it.techStack,
                            bulletPoints = it.bulletPoints
                        )
                    },
                    certifications = certifications.map {
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
                        layoutDensity = layoutDensity,
                        sectionOrder = sectionOrder.toList()
                    )
                )

                val success = ResumePdfExporter.exportPdf(context, profileDto)
                onComplete(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _exporting.value = false
            }
        }
    }
}
