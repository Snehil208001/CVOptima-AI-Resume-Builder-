package com.resumebuilder.backend.controller;

import com.resumebuilder.backend.dto.EducationDTO;
import com.resumebuilder.backend.dto.ExperienceDTO;
import com.resumebuilder.backend.dto.SkillDTO;
import com.resumebuilder.backend.dto.UserProfileDTO;
import com.resumebuilder.backend.entity.Education;
import com.resumebuilder.backend.entity.Experience;
import com.resumebuilder.backend.entity.Resume;
import com.resumebuilder.backend.entity.Skill;
import com.resumebuilder.backend.entity.User;
import com.resumebuilder.backend.repository.EducationRepository;
import com.resumebuilder.backend.repository.ExperienceRepository;
import com.resumebuilder.backend.repository.ResumeRepository;
import com.resumebuilder.backend.repository.SkillRepository;
import com.resumebuilder.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;

    public ProfileController(UserRepository userRepository,
                             ResumeRepository resumeRepository,
                             ExperienceRepository experienceRepository,
                             EducationRepository educationRepository,
                             SkillRepository skillRepository) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.skillRepository = skillRepository;
    }

    @GetMapping
    @Transactional
    public ResponseEntity<UserProfileDTO> getProfile() {
        User user = getOrCreateDefaultUser();
        Resume resume = getOrCreateDefaultResume(user);
        UserProfileDTO dto = mapToDTO(user, resume);
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<UserProfileDTO> updateProfile(@Valid @RequestBody UserProfileDTO dto) {
        User user = getOrCreateDefaultUser();

        // Update user personal details
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());

        Resume resume = getOrCreateDefaultResume(user);

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
                        .description(expDto.getDescription())
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
                        .resume(resume)
                        .build();
                resume.getEducations().add(edu);
            }
        }

        // Full replace: Clear and rebuild skills
        resume.getSkills().clear();
        if (dto.getSkills() != null) {
            for (SkillDTO skDto : dto.getSkills()) {
                Skill sk = Skill.builder()
                        .name(skDto.getName())
                        .proficiencyLevel(skDto.getProficiencyLevel())
                        .resume(resume)
                        .build();
                resume.getSkills().add(sk);
            }
        }

        userRepository.save(user);

        UserProfileDTO responseDto = mapToDTO(user, resume);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/experience")
    @Transactional
    public ResponseEntity<ExperienceDTO> saveExperience(@Valid @RequestBody ExperienceDTO dto) {
        User user = getOrCreateDefaultUser();
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
                .description(experience.getDescription())
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/skills")
    @Transactional
    public ResponseEntity<List<SkillDTO>> updateSkills(@Valid @RequestBody List<SkillDTO> skillDtos) {
        User user = getOrCreateDefaultUser();
        Resume resume = getOrCreateDefaultResume(user);

        // Find existing skills using SkillRepository
        List<Skill> existingSkills = skillRepository.findByResumeId(resume.getId());

        // Remove from resume's collection to avoid session sync mismatch
        resume.getSkills().clear();

        // Delete existing skills via SkillRepository
        skillRepository.deleteAll(existingSkills);

        List<Skill> newSkills = new ArrayList<>();
        if (skillDtos != null) {
            for (SkillDTO skDto : skillDtos) {
                Skill sk = Skill.builder()
                        .name(skDto.getName())
                        .proficiencyLevel(skDto.getProficiencyLevel())
                        .resume(resume)
                        .build();
                newSkills.add(sk);
            }
        }

        // Save new skills via SkillRepository
        List<Skill> savedSkills = skillRepository.saveAll(newSkills);
        resume.getSkills().addAll(savedSkills);

        List<SkillDTO> response = savedSkills.stream()
                .map(sk -> SkillDTO.builder()
                        .id(sk.getId())
                        .name(sk.getName())
                        .proficiencyLevel(sk.getProficiencyLevel())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/experience/{expId}")
    @Transactional
    public ResponseEntity<Void> deleteExperience(@PathVariable Long expId) {
        User user = getOrCreateDefaultUser();
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


    private User getOrCreateDefaultUser() {
        // Look up by default ID 1, otherwise find first user, or create one
        return userRepository.findById(1L).orElseGet(() -> {
            List<User> allUsers = userRepository.findAll();
            if (!allUsers.isEmpty()) {
                return allUsers.get(0);
            }
            User defaultUser = User.builder()
                    .username("default_user")
                    .email("default@example.com")
                    .password("password")
                    .firstName("John")
                    .lastName("Doe")
                    .resumes(new ArrayList<>())
                    .build();
            return userRepository.save(defaultUser);
        });
    }

    private Resume getOrCreateDefaultResume(User user) {
        if (user.getResumes() == null || user.getResumes().isEmpty()) {
            Resume defaultResume = Resume.builder()
                    .title("Default Resume")
                    .summary("Professional Summary")
                    .user(user)
                    .experiences(new ArrayList<>())
                    .educations(new ArrayList<>())
                    .skills(new ArrayList<>())
                    .build();
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
                        .build())
                .toList();

        List<SkillDTO> skillDTOs = resume.getSkills().stream()
                .map(sk -> SkillDTO.builder()
                        .id(sk.getId())
                        .name(sk.getName())
                        .proficiencyLevel(sk.getProficiencyLevel())
                        .build())
                .toList();

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .experiences(experienceDTOs)
                .educations(educationDTOs)
                .skills(skillDTOs)
                .build();
    }
}
