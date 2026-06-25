package com.resumebuilder.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.resumebuilder.backend.security.SecurityConfig;
import com.resumebuilder.backend.security.JwtAuthenticationFilter;

@WebMvcTest(
    value = ProfileController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
    )
)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ResumeRepository resumeRepository;

    @MockBean
    private ExperienceRepository experienceRepository;

    @MockBean
    private EducationRepository educationRepository;

    @MockBean
    private SkillRepository skillRepository;

    @Test
    void testGetProfile_Success() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .resumes(new ArrayList<>())
                .build();

        Resume resume = Resume.builder()
                .id(1L)
                .title("My Resume")
                .user(user)
                .experiences(new ArrayList<>())
                .educations(new ArrayList<>())
                .skills(new ArrayList<>())
                .build();
        user.addResume(resume);

        Experience exp = Experience.builder()
                .id(1L)
                .company("Google")
                .title("Software Engineer")
                .startDate(LocalDate.of(2020, 1, 1))
                .resume(resume)
                .build();
        resume.addExperience(exp);

        Education edu = Education.builder()
                .id(1L)
                .institution("MIT")
                .degree("MS")
                .fieldOfStudy("CS")
                .resume(resume)
                .build();
        resume.addEducation(edu);

        Skill skill = Skill.builder()
                .id(1L)
                .name("Java")
                .proficiencyLevel("Expert")
                .resume(resume)
                .build();
        resume.addSkill(skill);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.experiences[0].company").value("Google"))
                .andExpect(jsonPath("$.experiences[0].title").value("Software Engineer"))
                .andExpect(jsonPath("$.educations[0].institution").value("MIT"))
                .andExpect(jsonPath("$.educations[0].degree").value("MS"))
                .andExpect(jsonPath("$.skills[0].name").value("Java"));
    }

    @Test
    void testUpdateProfile_Success() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .resumes(new ArrayList<>())
                .build();

        Resume resume = Resume.builder()
                .id(1L)
                .title("My Resume")
                .user(user)
                .experiences(new ArrayList<>())
                .educations(new ArrayList<>())
                .skills(new ArrayList<>())
                .build();
        user.addResume(resume);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDTO dto = UserProfileDTO.builder()
                .username("new_john")
                .email("new_john@example.com")
                .firstName("Jonathan")
                .lastName("Doey")
                .experiences(Collections.singletonList(
                        ExperienceDTO.builder()
                                .company("Meta")
                                .title("Senior Staff")
                                .build()
                ))
                .educations(Collections.singletonList(
                        EducationDTO.builder()
                                .institution("Stanford")
                                .degree("PhD")
                                .build()
                ))
                .skills(Collections.singletonList(
                        SkillDTO.builder()
                                .name("Go")
                                .proficiencyLevel("Advanced")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_john"))
                .andExpect(jsonPath("$.email").value("new_john@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jonathan"))
                .andExpect(jsonPath("$.lastName").value("Doey"))
                .andExpect(jsonPath("$.experiences[0].company").value("Meta"))
                .andExpect(jsonPath("$.educations[0].institution").value("Stanford"))
                .andExpect(jsonPath("$.skills[0].name").value("Go"));
    }

    @Test
    void testUpdateProfile_ValidationFailure() throws Exception {
        UserProfileDTO dto = UserProfileDTO.builder()
                .username("")
                .email("not-an-email")
                .build();

        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void testSaveExperience_Create_Success() throws Exception {
        User user = User.builder().id(1L).resumes(new ArrayList<>()).build();
        Resume resume = Resume.builder().id(1L).user(user).experiences(new ArrayList<>()).build();
        user.addResume(resume);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> {
            Experience exp = inv.getArgument(0);
            exp.setId(10L);
            return exp;
        });

        ExperienceDTO dto = ExperienceDTO.builder()
                .company("Microsoft")
                .title("SWE II")
                .build();

        mockMvc.perform(patch("/api/v1/profile/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.company").value("Microsoft"))
                .andExpect(jsonPath("$.title").value("SWE II"));
    }

    @Test
    void testSaveExperience_Update_Success() throws Exception {
        User user = User.builder().id(1L).resumes(new ArrayList<>()).build();
        Resume resume = Resume.builder().id(1L).user(user).experiences(new ArrayList<>()).build();
        user.addResume(resume);

        Experience existingExp = Experience.builder()
                .id(10L)
                .company("Microsoft")
                .title("SWE II")
                .resume(resume)
                .build();
        resume.addExperience(existingExp);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(experienceRepository.findById(10L)).thenReturn(Optional.of(existingExp));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> inv.getArgument(0));

        ExperienceDTO dto = ExperienceDTO.builder()
                .id(10L)
                .company("Microsoft Inc")
                .title("Senior SWE")
                .build();

        mockMvc.perform(patch("/api/v1/profile/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.company").value("Microsoft Inc"))
                .andExpect(jsonPath("$.title").value("Senior SWE"));
    }

    @Test
    void testSaveExperience_NotFound() throws Exception {
        User user = User.builder().id(1L).resumes(new ArrayList<>()).build();
        Resume resume = Resume.builder().id(1L).user(user).build();
        user.addResume(resume);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(experienceRepository.findById(99L)).thenReturn(Optional.empty());

        ExperienceDTO dto = ExperienceDTO.builder()
                .id(99L)
                .company("Microsoft")
                .title("SWE II")
                .build();

        mockMvc.perform(patch("/api/v1/profile/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Experience with ID 99 not found"));
    }

    @Test
    void testUpdateSkills_Success() throws Exception {
        User user = User.builder().id(1L).resumes(new ArrayList<>()).build();
        Resume resume = Resume.builder().id(1L).user(user).skills(new ArrayList<>()).build();
        user.addResume(resume);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findByResumeId(any())).thenReturn(new ArrayList<>());
        when(skillRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        java.util.List<SkillDTO> skills = Collections.singletonList(
                SkillDTO.builder().name("Java").proficiencyLevel("Expert").build()
        );

        mockMvc.perform(patch("/api/v1/profile/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skills)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].proficiencyLevel").value("Expert"));
    }

    @Test
    void testDeleteExperience_Success() throws Exception {
        User user = User.builder().id(1L).resumes(new ArrayList<>()).build();
        Resume resume = Resume.builder().id(1L).user(user).experiences(new ArrayList<>()).build();
        user.addResume(resume);

        Experience exp = Experience.builder().id(10L).resume(resume).build();
        resume.addExperience(exp);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(experienceRepository.findById(10L)).thenReturn(Optional.of(exp));

        mockMvc.perform(delete("/api/v1/profile/experience/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteExperience_NotFound() throws Exception {
        User user = User.builder().id(1L).resumes(new ArrayList<>()).build();
        Resume resume = Resume.builder().id(1L).user(user).build();
        user.addResume(resume);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(experienceRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/profile/experience/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Experience with ID 99 not found"));
    }
}
