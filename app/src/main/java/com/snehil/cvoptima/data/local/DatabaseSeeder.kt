package com.snehil.cvoptima.data.local

import com.snehil.cvoptima.data.local.dao.*
import com.snehil.cvoptima.data.local.entity.*

object DatabaseSeeder {
    suspend fun seedIfNeeded(
        basicInfoDao: BasicInfoDao,
        educationDao: EducationDao,
        experienceDao: ExperienceDao,
        skillGroupDao: SkillGroupDao,
        projectDao: ProjectDao,
        certificationDao: CertificationDao,
        layoutSettingsDao: LayoutSettingsDao
    ) {
        val existingInfo = basicInfoDao.getByResumeId(1L)
        if (existingInfo != null) return

        // 1. Basic Info
        basicInfoDao.insert(
            LocalBasicInfo(
                id = 1,
                name = "SNEHIL",
                email = "snehil7542@gmail.com",
                contactNumber = "+91 7542957884",
                linkedinUrl = "https://linkedin.com/in/snehil7542",
                githubUrl = "https://github.com/Snehil208001",
                portfolioUrl = "https://snehil.dev",
                location = "Pune, India",
                professionalSummary = "Backend Engineer skilled in Java, Spring Boot, and microservices. Strong in REST APIs, databases, and DSA-based problem solving.",
                resumeId = 1L
            )
        )

        // 2. Education
        educationDao.insertAll(
            listOf(
                LocalEducation(
                    id = 1,
                    institution = "Bharati Vidyapeeth College Of Engineering",
                    degree = "Bachelor of Technology (B.Tech)",
                    fieldOfStudy = "Civil Engineering",
                    startDate = "Jun 2019",
                    endDate = "May 2023",
                    gpa = null,
                    score = null,
                    location = "Pune",
                    resumeId = 1L
                )
            )
        )

        // 3. Experiences
        experienceDao.insertAll(
            listOf(
                LocalExperience(
                    id = 1,
                    company = "Invyu Solution Technology (Remote)",
                    title = "Software Developer",
                    startDate = "Sep 2025",
                    endDate = "Feb 2026",
                    isCurrentRole = false,
                    location = "Remote",
                    type = "Software Developer",
                    bulletPoints = listOf(
                        "Built TRide with scalable RESTful APIs and real-time driver tracking via Google Maps SDK, serving 50+ users.",
                        "Developed InWork HRMS backend automating attendance, geolocation, and leave workflows — saving ~2 hrs/day.",
                        "Implemented 3-role access control (user, driver, admin) using Spring Security, JWT, and RBAC.",
                        "Deployed both apps via CI/CD pipelines within 5 months using MVVM architecture."
                    ),
                    description = "",
                    resumeId = 1L
                ),
                LocalExperience(
                    id = 2,
                    company = "Chamberly AB (Remote, Sweden)",
                    title = "Software Developer",
                    startDate = "Jun 2025",
                    endDate = "Sep 2025",
                    isCurrentRole = false,
                    location = "Remote, Sweden",
                    type = "Software Developer",
                    bulletPoints = listOf(
                        "Integrated Firebase Realtime Database and Retrofit API layer with zero data inconsistency across concurrent live sessions.",
                        "Built and maintained RESTful API integrations with optimized query performance for an international user base.",
                        "Delivered backend features on schedule via GitHub Actions CI/CD, collaborating with a remote team in Sweden."
                    ),
                    description = "",
                    resumeId = 1L
                )
            )
        )

        // 4. Skill Groups
        skillGroupDao.insertAll(
            listOf(
                LocalSkillGroup(
                    id = 1,
                    label = "Languages",
                    skills = listOf("Java", "Kotlin"),
                    resumeId = 1L
                ),
                LocalSkillGroup(
                    id = 2,
                    label = "Frameworks",
                    skills = listOf("Spring Boot", "Spring Security", "Spring Cloud", "Spring AI", "Hibernate/JPA", "Jetpack Compose", "React", "Next.js"),
                    resumeId = 1L
                ),
                LocalSkillGroup(
                    id = 3,
                    label = "Backend Core Skills",
                    skills = listOf("Microservices", "REST APIs", "PostgreSQL", "Redis", "Kafka", "JWT Authentication", "JUnit", "Mockito", "WebSockets", "OAuth2"),
                    resumeId = 1L
                ),
                LocalSkillGroup(
                    id = 4,
                    label = "Platforms",
                    skills = listOf("AWS", "Kubernetes", "Docker", "AWS S3", "CodePipeline", "Google Cloud", "CI/CD Pipelines", "GitHub Actions"),
                    resumeId = 1L
                ),
                LocalSkillGroup(
                    id = 5,
                    label = "Tools",
                    skills = listOf("Git", "Maven", "Postman", "Swagger", "Retrofit", "Firebase"),
                    resumeId = 1L
                ),
                LocalSkillGroup(
                    id = 6,
                    label = "AI & LLM",
                    skills = listOf("RAG", "LLM", "Tool Calling", "Vector Store", "ETL Pipeline", "Spring AI"),
                    resumeId = 1L
                )
            )
        )

        // 5. Projects
        projectDao.insertAll(
            listOf(
                LocalProject(
                    id = 1,
                    title = "Lovable Clone Project",
                    link = "https://github.com/Snehil208001/lovable-clone",
                    date = "Apr 2026",
                    techStack = "Spring Boot, Spring AI, Open Router, Redis, NodeJs, Kubernetes, Microservices",
                    bulletPoints = listOf(
                        "Architected a fault-tolerant, horizontally scalable microservices platform automating full-stack web application generation via LLMs, replicating capabilities of industry leaders like Lovable and v0.",
                        "Built a custom reverse proxy in Node.js with Redis-backed dynamic wildcard routing (*.app.domain.com), supporting 10,000+ ephemeral subdomain-to-cluster-IP mappings at <10 ms lookup latency.",
                        "Optimized LLM context efficiency by implementing tool calling, reducing input token usage by 50% and cutting end-to-end response latency by 50%."
                    ),
                    resumeId = 1L
                ),
                LocalProject(
                    id = 2,
                    title = "ProLink — Full-Stack Social Network",
                    link = "https://github.com/Snehil208001/prolink",
                    date = "Mar 2026",
                    techStack = "Spring Boot, React, Neo4j, MongoDB, WebSockets, Spring Cloud",
                    bulletPoints = listOf(
                        "Engineered a horizontally scalable, distributed LinkedIn-style platform using Spring Boot microservices with independent service deployment, load balancing, and a React frontend.",
                        "Designed a high-performance user graph using Neo4j, enabling complex relationship queries (2nd/3rd degree connections, mutual friends) that would be prohibitively expensive in a relational database.",
                        "Implemented real-time messaging via WebSockets with MongoDB storing unstructured chat history, and secured all inter-service communication via Spring Cloud API Gateway, Eureka service discovery, and JWT."
                    ),
                    resumeId = 1L
                ),
                LocalProject(
                    id = 3,
                    title = "Moonlight Stays (Airbnb-style Hotel Booking Platform)",
                    link = "https://github.com/Snehil208001/moonlight-stays",
                    date = "Jan 2026",
                    techStack = "Spring Boot, PostgreSQL, AWS, Stripe, JWT, Hibernate",
                    bulletPoints = listOf(
                        "Built a high-availability, production-grade hotel booking platform with Spring Boot 3.5 backend and Next.js 14 frontend, deployed on AWS using Amplify, Elastic Beanstalk, and RDS.",
                        "Implemented a dynamic pricing engine using the Strategy Design Pattern — enabling surge, holiday, and occupancy-based pricing as swappable strategies without modifying core booking logic.",
                        "Integrated Stripe Checkout with server-side webhook verification to ensure payment confirmation is never trusted from the client side, preventing fraudulent booking confirmation attacks.",
                        "Secured the API with Spring Security + JWT refresh token rotation and role-based access control, enforcing strict separation between Guest and Hotel Manager permissions."
                    ),
                    resumeId = 1L
                )
            )
        )

        // 6. Certifications (used as Achievements)
        certificationDao.insertAll(
            listOf(
                LocalCertification(
                    id = 1,
                    title = "Achievements",
                    issuer = "Problem Solving",
                    link = null,
                    date = "",
                    bulletPoints = listOf(
                        "Problem Solving: - Solved 350+ DSA problems on LeetCode",
                        "- Strong in Arrays, Trees, Graphs, DP",
                        "- Good understanding of time and space complexity"
                    ),
                    resumeId = 1L
                )
            )
        )

        // 7. Layout settings
        layoutSettingsDao.insert(
            LocalLayoutSettings(
                id = 1,
                layoutDensity = "Normal",
                sectionOrder = listOf("Skills", "Work Experiences", "Projects", "Certifications", "Education"),
                resumeId = 1L
            )
        )
    }
}
