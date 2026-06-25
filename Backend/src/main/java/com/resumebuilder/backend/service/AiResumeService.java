package com.resumebuilder.backend.service;

import com.resumebuilder.backend.dto.ExperienceOptimizationRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiResumeService {

    private final ChatClient chatClient;
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private static final String SYSTEM_PROMPT = """
        You are an expert ATS (Applicant Tracking System) optimizer and professional resume writer.
        Your task is to rewrite the user's raw job experiences to align perfectly with the target Job Description, maximizing their match score while maintaining absolute truthfulness.
        
        Follow these instructions strictly:
        1. Rewrite the experience as a clean list of bullet points starting with '• '.
        2. Start each bullet point with a strong, active verb in the appropriate tense (past tense for past jobs, present tense for current jobs).
        3. Incorporate relevant keywords and technical skills from the target Job Description naturally where appropriate.
        4. Implement the Google X-Y-Z formula wherever possible: "Accomplished [X] as measured by [Y], by doing [Z]". Quantify achievements (e.g., percentages, dollar amounts, hours saved, team efficiency gains) based on the context.
        5. Keep the tone professional, impact-driven, and highly concise.
        6. Do NOT include any introductory or concluding remarks, explanations, or metadata. Output ONLY the optimized bullet points.
        """;

    public AiResumeService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Optimizes a user's raw experience bullet points to match the job description and be ATS-friendly.
     *
     * @param rawExperience  The raw work history or bullet points provided by the user.
     * @param jobDescription The target job description to align against.
     * @return ATS-optimized experience bullet points.
     */
    public String optimizeExperienceForAts(String rawExperience, String jobDescription) {
        if (rawExperience == null || rawExperience.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw experience text cannot be null or empty");
        }
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Job description cannot be null or empty");
        }

        String userPrompt = String.format("""
            Target Job Description:
            %s
            
            Raw Experience Text:
            %s
            """, jobDescription.trim(), rawExperience.trim());

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * Starts an asynchronous resume optimization task and returns the unique taskId.
     *
     * @param request The optimization request parameters.
     * @return The unique UUID of the started task.
     */
    public String startOptimizationTask(ExperienceOptimizationRequest request) {
        if (request.getRawExperience() == null || request.getRawExperience().trim().isEmpty()) {
            throw new IllegalArgumentException("Raw experience text cannot be null or empty");
        }
        if (request.getTargetJobDescription() == null || request.getTargetJobDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Job description cannot be null or empty");
        }

        String taskId = UUID.randomUUID().toString();
        
        // Emitter timeout set to 3 minutes
        SseEmitter emitter = new SseEmitter(180000L);
        emitterMap.put(taskId, emitter);

        // Cleanup handlers
        emitter.onCompletion(() -> emitterMap.remove(taskId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitterMap.remove(taskId);
        });
        emitter.onError((ex) -> emitterMap.remove(taskId));

        // Start background processing thread using Virtual Thread Executor
        executorService.submit(() -> {
            try {
                String userPrompt = String.format("""
                    Target Job Description:
                    %s
                    
                    Raw Experience Text:
                    %s
                    """, request.getTargetJobDescription().trim(), request.getRawExperience().trim());

                Flux<String> contentFlux = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(userPrompt)
                        .stream()
                        .content();

                for (String chunk : contentFlux.toIterable()) {
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(chunk));
                }

                // Send done event and complete
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(""));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                emitterMap.remove(taskId);
            }
        });

        return taskId;
    }

    /**
     * Retrieves the SseEmitter associated with the given taskId.
     *
     * @param taskId The task ID.
     * @return The SseEmitter, or null if not found.
     */
    public SseEmitter getEmitter(String taskId) {
        return emitterMap.get(taskId);
    }
}
