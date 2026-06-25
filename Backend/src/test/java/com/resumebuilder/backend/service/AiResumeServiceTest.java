package com.resumebuilder.backend.service;

import com.resumebuilder.backend.dto.ExperienceOptimizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AiResumeServiceTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private AiResumeService aiResumeService;

    @BeforeEach
    void setUp() {
        chatClientBuilder = mock(ChatClient.Builder.class);
        // Using deep stubs to easily mock the fluent ChatClient API
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        aiResumeService = new AiResumeService(chatClientBuilder);
    }

    @Test
    void testOptimizeExperienceForAts_Success() {
        String expectedOutput = "• Optimized Java APIs reducing latency by 20% using Spring Boot caching.";
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content())
                .thenReturn(expectedOutput);

        String result = aiResumeService.optimizeExperienceForAts(
                "I worked on some Java code and made things faster.",
                "Java Engineer with expertise in performance optimization and caching."
        );

        assertNotNull(result);
        assertEquals(expectedOutput, result);
    }

    @Test
    void testOptimizeExperienceForAts_NullOrEmptyRawExperience_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            aiResumeService.optimizeExperienceForAts("", "Target Job Description")
        );
        assertThrows(IllegalArgumentException.class, () -> 
            aiResumeService.optimizeExperienceForAts(null, "Target Job Description")
        );
    }

    @Test
    void testOptimizeExperienceForAts_NullOrEmptyJobDescription_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            aiResumeService.optimizeExperienceForAts("Raw Experience", "")
        );
        assertThrows(IllegalArgumentException.class, () -> 
            aiResumeService.optimizeExperienceForAts("Raw Experience", null)
        );
    }

    @Test
    void testStartOptimizationTask_Success_CleansUpMap() throws Exception {
        ExperienceOptimizationRequest request = ExperienceOptimizationRequest.builder()
                .rawExperience("I worked on Java code.")
                .targetJobDescription("Java developer role.")
                .build();

        Flux<String> mockFlux = Flux.just("• Optimized ", "Java APIs.");
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .stream()
                .content())
                .thenReturn(mockFlux);

        String taskId = aiResumeService.startOptimizationTask(request);
        assertNotNull(taskId);

        SseEmitter emitter = aiResumeService.getEmitter(taskId);
        assertNotNull(emitter);

        // Wait for the background thread and flux to finish processing and cleanup the map
        int retries = 100;
        while (aiResumeService.getEmitter(taskId) != null && retries > 0) {
            Thread.sleep(10);
            retries--;
        }

        assertNull(aiResumeService.getEmitter(taskId), "Emitter should have been removed from the map upon completion");
    }

    @Test
    void testStartOptimizationTask_AiError_CleansUpMap() throws Exception {
        ExperienceOptimizationRequest request = ExperienceOptimizationRequest.builder()
                .rawExperience("I worked on Java code.")
                .targetJobDescription("Java developer role.")
                .build();

        Flux<String> mockFlux = Flux.error(new RuntimeException("OpenAI connection failed"));
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .stream()
                .content())
                .thenReturn(mockFlux);

        String taskId = aiResumeService.startOptimizationTask(request);
        assertNotNull(taskId);

        SseEmitter emitter = aiResumeService.getEmitter(taskId);
        assertNotNull(emitter);

        // Wait for the background thread to handle error and cleanup the map
        int retries = 100;
        while (aiResumeService.getEmitter(taskId) != null && retries > 0) {
            Thread.sleep(10);
            retries--;
        }

        assertNull(aiResumeService.getEmitter(taskId), "Emitter should have been removed from the map upon error");
    }
}
