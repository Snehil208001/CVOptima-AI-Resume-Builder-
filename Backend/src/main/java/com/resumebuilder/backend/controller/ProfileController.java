package com.resumebuilder.backend.controller;

import com.resumebuilder.backend.dto.*;
import com.resumebuilder.backend.entity.*;
import com.resumebuilder.backend.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SkillGroupRepository skillGroupRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final LayoutConfigRepository layoutConfigRepository;

    public ProfileController(UserRepository userRepository,
                             ResumeRepository resumeRepository,
                             ExperienceRepository experienceRepository,
                             EducationRepository educationRepository,
                             SkillGroupRepository skillGroupRepository,
                             ProjectRepository projectRepository,
                             CertificationRepository certificationRepository,
                             LayoutConfigRepository layoutConfigRepository) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.skillGroupRepository = skillGroupRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
        this.layoutConfigRepository = layoutConfigRepository;
    }

    @GetMapping
    @Transactional
    public ResponseEntity<UserProfileDTO> getProfile(java.security.Principal principal) {
        User user = getUserFromPrincipal(principal);
        Resume resume = getOrCreateDefaultResume(user);
        UserProfileDTO dto = mapToDTO(user, resume);
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<UserProfileDTO> updateProfile(java.security.Principal principal, @Valid @RequestBody UserProfileDTO dto) {
        return syncProfile(principal, dto);
    }

    @PutMapping("/sync")
    @Transactional
    public ResponseEntity<UserProfileDTO> syncProfile(java.security.Principal principal, @Valid @RequestBody UserProfileDTO dto) {
        User user = getUserFromPrincipal(principal);

        // Update user personal details
        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        user.setName(dto.getName());
        user.setContactNumber(dto.getContactNumber());
        user.setLinkedinUrl(dto.getLinkedinUrl());
        user.setGithubUrl(dto.getGithubUrl());
        user.setPortfolioUrl(dto.getPortfolioUrl());
        user.setProfessionalSummary(dto.getProfessionalSummary());

        Resume resume = getOrCreateDefaultResume(user);
        resume.setSummary(dto.getProfessionalSummary());

        // Full replace: Clear and rebuild experiences
        resume.getExperiences().clear();
        if (dto.getExperiences() != null) {
            for (ExperienceDTO expDto : dto.getExperiences()) {
                Experience exp = Experience.builder()
                        .company(expDto.getCompany())
                        .title(expDto.getTitle())
                        .startDate(expDto.getStartDate())
                        .endDate(expDto.getEndDate())
                        .isCurrentRole(expDto.getIsCurrentRole())
                        .location(expDto.getLocation())
                        .type(expDto.getType())
                        .bulletPoints(expDto.getBulletPoints() != null ? new ArrayList<>(expDto.getBulletPoints()) : new ArrayList<>())
                        .resume(resume)
                        .build();
                resume.getExperiences().add(exp);
            }
        }

        // Full replace: Clear and rebuild educations
        resume.getEducations().clear();
        if (dto.getEducations() != null) {
            for (EducationDTO eduDto : dto.getEducations()) {
                Education edu = Education.builder()
                        .institution(eduDto.getInstitution())
                        .degree(eduDto.getDegree())
                        .fieldOfStudy(eduDto.getFieldOfStudy())
                        .startDate(eduDto.getStartDate())
                        .endDate(eduDto.getEndDate())
                        .gpa(eduDto.getGpa())
                        .score(eduDto.getScore())
                        .location(eduDto.getLocation())
                        .resume(resume)
                        .build();
                resume.getEducations().add(edu);
            }
        }

        // Full replace: Clear and rebuild skill groups
        resume.getSkillGroups().clear();
        if (dto.getSkillGroups() != null) {
            for (SkillGroupDTO sgDto : dto.getSkillGroups()) {
                SkillGroup sg = SkillGroup.builder()
                        .label(sgDto.getLabel())
                        .skills(sgDto.getSkills() != null ? new ArrayList<>(sgDto.getSkills()) : new ArrayList<>())
                        .resume(resume)
                        .build();
                resume.getSkillGroups().add(sg);
            }
        }

        // Full replace: Clear and rebuild projects
        resume.getProjects().clear();
        if (dto.getProjects() != null) {
            for (ProjectDTO projDto : dto.getProjects()) {
                Project proj = Project.builder()
                        .title(projDto.getTitle())
                        .link(projDto.getLink())
                        .date(projDto.getDate())
                        .techStack(projDto.getTechStack())
                        .bulletPoints(projDto.getBulletPoints() != null ? new ArrayList<>(projDto.getBulletPoints()) : new ArrayList<>())
                        .resume(resume)
                        .build();
                resume.getProjects().add(proj);
            }
        }

        // Full replace: Clear and rebuild certifications
        resume.getCertifications().clear();
        if (dto.getCertifications() != null) {
            for (CertificationDTO certDto : dto.getCertifications()) {
                Certification cert = Certification.builder()
                        .title(certDto.getTitle())
                        .issuer(certDto.getIssuer())
                        .link(certDto.getLink())
                        .date(certDto.getDate())
                        .bulletPoints(certDto.getBulletPoints() != null ? new ArrayList<>(certDto.getBulletPoints()) : new ArrayList<>())
                        .resume(resume)
                        .build();
                resume.getCertifications().add(cert);
            }
        }

        // Sync layout configuration
        if (dto.getLayoutConfig() != null) {
            LayoutConfig lc = resume.getLayoutConfig();
            if (lc == null) {
                lc = new LayoutConfig();
                lc.setResume(resume);
                resume.setLayoutConfig(lc);
            }
            lc.setLayoutDensity(dto.getLayoutConfig().getLayoutDensity());
            lc.setSectionOrder(dto.getLayoutConfig().getSectionOrder() != null ? new ArrayList<>(dto.getLayoutConfig().getSectionOrder()) : new ArrayList<>());
        } else {
            resume.setLayoutConfig(null);
        }

        userRepository.save(user);

        UserProfileDTO responseDto = mapToDTO(user, resume);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/experience")
    @Transactional
    public ResponseEntity<ExperienceDTO> saveExperience(java.security.Principal principal, @Valid @RequestBody ExperienceDTO dto) {
        User user = getUserFromPrincipal(principal);
        Resume resume = getOrCreateDefaultResume(user);

        Experience experience;
        if (dto.getId() == null) {
            // Create new experience card
            experience = Experience.builder()
                    .company(dto.getCompany())
                    .title(dto.getTitle())
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .isCurrentRole(dto.getIsCurrentRole())
                    .location(dto.getLocation())
                    .type(dto.getType())
                    .bulletPoints(dto.getBulletPoints() != null ? new ArrayList<>(dto.getBulletPoints()) : new ArrayList<>())
                    .description(dto.getDescription())
                    .resume(resume)
                    .build();
            experience = experienceRepository.save(experience);
            resume.getExperiences().add(experience);
        } else {
            // Update existing experience card
            experience = experienceRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Experience with ID " + dto.getId() + " not found"));

            if (!experience.getResume().getId().equals(resume.getId())) {
                throw new IllegalArgumentException("Experience does not belong to this profile");
            }

            experience.setCompany(dto.getCompany());
            experience.setTitle(dto.getTitle());
            experience.setStartDate(dto.getStartDate());
            experience.setEndDate(dto.getEndDate());
            experience.setIsCurrentRole(dto.getIsCurrentRole());
            experience.setLocation(dto.getLocation());
            experience.setType(dto.getType());
            experience.setBulletPoints(dto.getBulletPoints() != null ? new ArrayList<>(dto.getBulletPoints()) : new ArrayList<>());
            experience.setDescription(dto.getDescription());
            experience = experienceRepository.save(experience);
        }

        ExperienceDTO responseDto = ExperienceDTO.builder()
                .id(experience.getId())
                .company(experience.getCompany())
                .title(experience.getTitle())
                .startDate(experience.getStartDate())
                .endDate(experience.getEndDate())
                .isCurrentRole(experience.getIsCurrentRole())
                .location(experience.getLocation())
                .type(experience.getType())
                .bulletPoints(experience.getBulletPoints())
                .description(experience.getDescription())
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/experience/{expId}")
    @Transactional
    public ResponseEntity<Void> deleteExperience(java.security.Principal principal, @PathVariable Long expId) {
        User user = getUserFromPrincipal(principal);
        Resume resume = getOrCreateDefaultResume(user);

        Experience experience = experienceRepository.findById(expId)
                .orElseThrow(() -> new IllegalArgumentException("Experience with ID " + expId + " not found"));

        if (!experience.getResume().getId().equals(resume.getId())) {
            throw new IllegalArgumentException("Experience does not belong to this profile");
        }

        resume.getExperiences().remove(experience);
        experienceRepository.delete(experience);

        return ResponseEntity.noContent().build();
    }

    private User getUserFromPrincipal(java.security.Principal principal) {
        if (principal != null) {
            return userRepository.findByUsername(principal.getName())
                    .orElseGet(this::getOrCreateDefaultUser);
        }
        return getOrCreateDefaultUser();
    }

    private User getOrCreateDefaultUser() {
        return userRepository.findById(1L).orElseGet(() -> {
            List<User> allUsers = userRepository.findAll();
            if (!allUsers.isEmpty()) {
                return allUsers.get(0);
            }
            User defaultUser = User.builder()
                    .username("default_user")
                    .email("default@example.com")
                    .password("password")
                    .firstName("Default")
                    .lastName("User")
                    .name("Default User")
                    .contactNumber("")
                    .linkedinUrl("")
                    .githubUrl("")
                    .portfolioUrl("")
                    .professionalSummary("Experienced professional.")
                    .resumes(new ArrayList<>())
                    .build();
            return userRepository.save(defaultUser);
        });
    }

    private Resume getOrCreateDefaultResume(User user) {
        if (user.getResumes() == null || user.getResumes().isEmpty()) {
            Resume defaultResume = Resume.builder()
                    .title("Default Resume")
                    .summary(user.getProfessionalSummary())
                    .user(user)
                    .experiences(new ArrayList<>())
                    .educations(new ArrayList<>())
                    .skillGroups(new ArrayList<>())
                    .projects(new ArrayList<>())
                    .certifications(new ArrayList<>())
                    .build();

            defaultResume.setLayoutConfig(
                LayoutConfig.builder()
                    .layoutDensity("Normal")
                    .sectionOrder(new ArrayList<>(java.util.Arrays.asList("Skills", "Work Experiences", "Projects", "Certifications", "Education")))
                    .resume(defaultResume)
                    .build()
            );

            defaultResume = resumeRepository.save(defaultResume);
            user.getResumes().add(defaultResume);
            return defaultResume;
        }
        return user.getResumes().get(0);
    }

    private UserProfileDTO mapToDTO(User user, Resume resume) {
        List<ExperienceDTO> experienceDTOs = resume.getExperiences().stream()
                .map(exp -> ExperienceDTO.builder()
                        .id(exp.getId())
                        .company(exp.getCompany())
                        .title(exp.getTitle())
                        .startDate(exp.getStartDate())
                        .endDate(exp.getEndDate())
                        .isCurrentRole(exp.getIsCurrentRole())
                        .location(exp.getLocation())
                        .type(exp.getType())
                        .bulletPoints(exp.getBulletPoints())
                        .description(exp.getDescription())
                        .build())
                .toList();

        List<EducationDTO> educationDTOs = resume.getEducations().stream()
                .map(edu -> EducationDTO.builder()
                        .id(edu.getId())
                        .institution(edu.getInstitution())
                        .degree(edu.getDegree())
                        .fieldOfStudy(edu.getFieldOfStudy())
                        .startDate(edu.getStartDate())
                        .endDate(edu.getEndDate())
                        .gpa(edu.getGpa())
                        .score(edu.getScore())
                        .location(edu.getLocation())
                        .build())
                .toList();

        List<SkillGroupDTO> skillGroupDTOs = resume.getSkillGroups().stream()
                .map(sg -> SkillGroupDTO.builder()
                        .id(sg.getId())
                        .label(sg.getLabel())
                        .skills(sg.getSkills())
                        .build())
                .toList();

        List<ProjectDTO> projectDTOs = resume.getProjects().stream()
                .map(proj -> ProjectDTO.builder()
                        .id(proj.getId())
                        .title(proj.getTitle())
                        .link(proj.getLink())
                        .date(proj.getDate())
                        .techStack(proj.getTechStack())
                        .bulletPoints(proj.getBulletPoints())
                        .build())
                .toList();

        List<CertificationDTO> certificationDTOs = resume.getCertifications().stream()
                .map(cert -> CertificationDTO.builder()
                        .id(cert.getId())
                        .title(cert.getTitle())
                        .issuer(cert.getIssuer())
                        .link(cert.getLink())
                        .date(cert.getDate())
                        .bulletPoints(cert.getBulletPoints())
                        .build())
                .toList();

        LayoutConfigDTO layoutConfigDTO = null;
        if (resume.getLayoutConfig() != null) {
            layoutConfigDTO = LayoutConfigDTO.builder()
                    .id(resume.getLayoutConfig().getId())
                    .layoutDensity(resume.getLayoutConfig().getLayoutDensity())
                    .sectionOrder(resume.getLayoutConfig().getSectionOrder())
                    .build();
        }

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .name(user.getName())
                .contactNumber(user.getContactNumber())
                .linkedinUrl(user.getLinkedinUrl())
                .githubUrl(user.getGithubUrl())
                .portfolioUrl(user.getPortfolioUrl())
                .professionalSummary(user.getProfessionalSummary())
                .experiences(experienceDTOs)
                .educations(educationDTOs)
                .skillGroups(skillGroupDTOs)
                .projects(projectDTOs)
                .certifications(certificationDTOs)
                .layoutConfig(layoutConfigDTO)
                .build();
    }
}
