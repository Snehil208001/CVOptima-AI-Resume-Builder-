package com.resumebuilder.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumebuilder.backend.dto.ExperienceOptimizationRequest;
import com.resumebuilder.backend.service.AiResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.resumebuilder.backend.security.SecurityConfig;
import com.resumebuilder.backend.security.JwtAuthenticationFilter;

@WebMvcTest(
    value = AiResumeController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
    )
)
class AiResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiResumeService aiResumeService;

    @Test
    void testOptimizeExperience_Success() throws Exception {
        ExperienceOptimizationRequest request = ExperienceOptimizationRequest.builder()
                .rawExperience("Designed APIs in Java.")
                .targetJobDescription("Looking for a Java developer.")
                .build();

        String fakeTaskId = "123e4567-e89b-12d3-a456-426614174000";
        when(aiResumeService.startOptimizationTask(any(ExperienceOptimizationRequest.class))).thenReturn(fakeTaskId);

        mockMvc.perform(post("/api/v1/ai/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.taskId").value(fakeTaskId))
                .andExpect(jsonPath("$.optimizedText").doesNotExist());
    }

    @Test
    void testOptimizeExperience_ValidationFailure() throws Exception {
        ExperienceOptimizationRequest request = ExperienceOptimizationRequest.builder()
                .rawExperience("")
                .targetJobDescription("")
                .build();

        mockMvc.perform(post("/api/v1/ai/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.validationErrors.rawExperience").exists())
                .andExpect(jsonPath("$.validationErrors.targetJobDescription").exists());
    }

    @Test
    void testOptimizeExperience_ServiceIllegalArgument() throws Exception {
        ExperienceOptimizationRequest request = ExperienceOptimizationRequest.builder()
                .rawExperience("valid exp")
                .targetJobDescription("valid jd")
                .build();

        when(aiResumeService.startOptimizationTask(any(ExperienceOptimizationRequest.class)))
                .thenThrow(new IllegalArgumentException("Service execution failed due to invalid arguments"));

        mockMvc.perform(post("/api/v1/ai/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Service execution failed due to invalid arguments"));
    }

    @Test
    void testStreamOptimization_Success() throws Exception {
        String fakeTaskId = "123e4567-e89b-12d3-a456-426614174000";
        SseEmitter emitter = new SseEmitter();
        when(aiResumeService.getEmitter(fakeTaskId)).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/ai/optimize/" + fakeTaskId + "/stream"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void testStreamOptimization_NotFound() throws Exception {
        String fakeTaskId = "non-existent-id";
        when(aiResumeService.getEmitter(fakeTaskId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/ai/optimize/" + fakeTaskId + "/stream"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Task ID not found or already completed"));
    }
}
