package com.resumebuilder.backend.service;

import com.resumebuilder.backend.dto.ExperienceOptimizationRequest;
import com.resumebuilder.backend.entity.*;
import com.resumebuilder.backend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final String apiKey;
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

    public AiResumeService(ChatClient.Builder chatClientBuilder,
                           UserRepository userRepository,
                           @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.apiKey = apiKey != null ? apiKey : "";
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
        return optimizeExperienceForAts(rawExperience, jobDescription, null);
    }

    public String optimizeExperienceForAts(String rawExperience, String jobDescription, String username) {
        if (rawExperience == null || rawExperience.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw experience text cannot be null or empty");
        }
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Job description cannot be null or empty");
        }

        String context = buildCandidateContext(username);

        String userPrompt = String.format("""
            Target Job Description:
            %s
            
            Raw Experience Text:
            %s
            %s
            """, jobDescription.trim(), rawExperience.trim(), context);

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
        return startOptimizationTask(request, null);
    }

    public String startOptimizationTask(ExperienceOptimizationRequest request, String username) {
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

        String context = buildCandidateContext(username);

        // Start background processing thread using Virtual Thread Executor
        executorService.submit(() -> {
            try {
                if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("dummy") || apiKey.contains("please-set")) {
                    String mockResponse = "• Restructured Android package module hierarchy to enforce clean separation of concerns, decreasing build times by 24%.\n" +
                            "• Integrated Jetpack Compose UI with unidirectional data flow (UDF), improving developer productivity and rendering performance.\n" +
                            "• Orchestrated background tasks using Kotlin Coroutines and WorkManager, resulting in a 40% reduction in database-related main-thread blocks.\n" +
                            "• Implemented robust error-handling policies and local DB fallback mechanisms, raising application reliability to 99.8% crash-free sessions.";
                    
                    String[] words = mockResponse.split(" ");
                    for (String word : words) {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(word + " "));
                        Thread.sleep(85); // Simulate typing speed
                    }
                    
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data(""));
                    emitter.complete();
                    return;
                }

                String userPrompt = String.format("""
                    Target Job Description:
                    %s
                    
                    Raw Experience Text:
                    %s
                    %s
                    """, request.getTargetJobDescription().trim(), request.getRawExperience().trim(), context);

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

    private String buildCandidateContext(String username) {
        if (username == null) {
            return "";
        }
        StringBuilder contextBuilder = new StringBuilder();
        userRepository.findByUsername(username).ifPresent(user -> {
            contextBuilder.append("\nCandidate Background Context:\n");
            if (user.getName() != null) contextBuilder.append("- Name: ").append(user.getName()).append("\n");
            if (user.getProfessionalSummary() != null) contextBuilder.append("- Professional Summary: ").append(user.getProfessionalSummary()).append("\n");
            
            if (user.getResumes() != null && !user.getResumes().isEmpty()) {
                Resume resume = user.getResumes().get(0);
                if (resume.getSkillGroups() != null && !resume.getSkillGroups().isEmpty()) {
                    contextBuilder.append("- Skill Groups:\n");
                    for (SkillGroup sg : resume.getSkillGroups()) {
                        contextBuilder.append("  * ").append(sg.getLabel()).append(": ").append(String.join(", ", sg.getSkills())).append("\n");
                    }
                }
                if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
                    contextBuilder.append("- Projects:\n");
                    for (Project p : resume.getProjects()) {
                        contextBuilder.append("  * ").append(p.getTitle());
                        if (p.getTechStack() != null) contextBuilder.append(" (Tech: ").append(p.getTechStack()).append(")");
                        if (p.getBulletPoints() != null && !p.getBulletPoints().isEmpty()) {
                            contextBuilder.append(" - ").append(String.join("; ", p.getBulletPoints()));
                        }
                        contextBuilder.append("\n");
                    }
                }
                if (resume.getCertifications() != null && !resume.getCertifications().isEmpty()) {
                    contextBuilder.append("- Certifications:\n");
                    for (Certification c : resume.getCertifications()) {
                        contextBuilder.append("  * ").append(c.getTitle()).append(" (Issuer: ").append(c.getIssuer()).append(")\n");
                    }
                }
                if (resume.getEducations() != null && !resume.getEducations().isEmpty()) {
                    contextBuilder.append("- Education:\n");
                    for (Education edu : resume.getEducations()) {
                        contextBuilder.append("  * ").append(edu.getDegree()).append(" in ").append(edu.getFieldOfStudy())
                                .append(" from ").append(edu.getInstitution()).append("\n");
                    }
                }
            }
        });
        return contextBuilder.toString();
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
