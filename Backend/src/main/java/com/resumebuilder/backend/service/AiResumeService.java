package com.resumebuilder.backend.service;

import com.resumebuilder.backend.dto.AtsAnalysisRequest;
import com.resumebuilder.backend.dto.AtsAnalysisResponse;
import com.resumebuilder.backend.dto.ExperienceOptimizationRequest;
import com.resumebuilder.backend.dto.SummaryGenerationRequest;
import com.resumebuilder.backend.entity.*;
import com.resumebuilder.backend.repository.UserRepository;
import com.resumebuilder.backend.repository.AiTaskStateRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiResumeService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AiResumeService.class);

    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final AiTaskStateRepository aiTaskStateRepository;
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

    private static final String ATS_ANALYSIS_SYSTEM_PROMPT = """
        You are an expert ATS (Applicant Tracking System) scanner used by Fortune 500 companies.
        Your job is to deeply analyze a resume's text content and provide a structured compatibility report.

        Evaluate the resume across these 4 categories:
        1. **Layout & Format** — Does it have a clear header (name, contact, links)? Is it single-column? Are sections well-organized (Summary, Skills, Experience, Projects, Education, Certifications)?
        2. **Keywords & Skills** — Does it contain relevant technical keywords, programming languages, frameworks, tools? Does the professional summary mention specialization and years of experience?
        3. **Metrics & Impact** — Do bullet points contain quantified achievements (percentages, dollar amounts, time saved, team sizes, user counts)?
        4. **Power Verbs & Writing Quality** — Do bullet points start with strong action verbs (Architected, Engineered, Optimized, Led, etc.)? Is the writing concise and professional?

        If a target job description is provided, evaluate keyword match against it specifically.

        SCORING RULES (be strict and realistic):
        - A resume with only a name and 2-3 skills should score 25-35.
        - A resume with basic info but no detailed experience bullets should score 35-48.
        - A resume with some experience but no metrics/numbers should score 48-62.
        - A well-structured resume with metrics but missing some sections should score 62-78.
        - A comprehensive resume with quantified achievements, strong verbs, all sections filled should score 78-92.
        - A perfect resume tailored to a specific JD with all optimizations should score 90-98.
        - NEVER give 100. NEVER give above 60 to a thin or sparse resume.

        You MUST respond with ONLY a valid JSON object. Do not include any markdown format, code fences, or headers. The output must be directly parseable as a JSON object.

        The JSON object must contain these exact keys:
        1. "score" - A numeric value (integer from 0 to 100).
        2. "rating" - A string matching exactly one of: Needs Significant Work, Moderate Compatibility, ATS-Friendly, Highly ATS Compatible.
        3. "layoutSuggestions" - An array of 2 to 5 string suggestions (each suggestion prefixing with ✅, ⚠️, or ❌).
        4. "keywordSuggestions" - An array of 2 to 5 string suggestions (each suggestion prefixing with ✅, ⚠️, or ❌).
        5. "metricSuggestions" - An array of 2 to 5 string suggestions (each suggestion prefixing with ✅, ⚠️, or ❌).
        6. "verbSuggestions" - An array of 2 to 5 string suggestions (each suggestion prefixing with ✅, ⚠️, or ❌).

        Be specific and actionable in suggestions. Reference actual content from the resume when possible.
        """;

    public AiResumeService(ChatClient.Builder chatClientBuilder,
                           UserRepository userRepository,
                           AiTaskStateRepository aiTaskStateRepository,
                           @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.aiTaskStateRepository = aiTaskStateRepository;
        this.apiKey = apiKey != null ? apiKey : "";
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Analyzes resume text for ATS compatibility using OpenAI.
     *
     * @param resumeText         The full text content of the resume.
     * @param targetJobDescription Optional target job description for tailored scoring.
     * @return AtsAnalysisResponse with score, rating, and categorized suggestions.
     */
    public AtsAnalysisResponse analyzeAtsScore(String resumeText, String targetJobDescription) {
        logger.info("Received ATS compatibility analysis request. Resume text length: {} characters.", 
                resumeText != null ? resumeText.length() : 0);

        if (resumeText == null || resumeText.trim().isEmpty()) {
            logger.warn("Received empty resume text for ATS analysis.");
            return AtsAnalysisResponse.builder()
                    .score(0)
                    .rating("Needs Significant Work")
                    .layoutSuggestions(List.of("❌ No resume content provided. Upload a resume or build one in the profile builder."))
                    .keywordSuggestions(List.of("❌ No keywords detected."))
                    .metricSuggestions(List.of("❌ No content to evaluate metrics."))
                    .verbSuggestions(List.of("❌ No content to evaluate writing quality."))
                    .build();
        }

        // If API key is not configured, return a heuristic mock response
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("dummy") || apiKey.contains("please-set")) {
            logger.warn("OpenAI API key is not configured (Value: '{}'). Falling back to local offline heuristic scanner.", apiKey);
            return buildMockAtsResponse(resumeText);
        }

        String userPrompt = "Analyze this resume for ATS compatibility:\n\n" + resumeText.trim();
        if (targetJobDescription != null && !targetJobDescription.trim().isEmpty()) {
            userPrompt += "\n\nTarget Job Description:\n" + targetJobDescription.trim();
        }

        try {
            logger.info("[OpenAI Request] Sending ATS analysis request.\nSystem Prompt: {}\nUser Prompt: {}", ATS_ANALYSIS_SYSTEM_PROMPT, userPrompt);
            String rawResponse = chatClient.prompt()
                    .system(ATS_ANALYSIS_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            logger.info("[OpenAI Response] Received raw response from OpenAI: {}", rawResponse);

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            // Strip markdown code fences if present
            String jsonStr = rawResponse.trim();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }

            JsonNode root = mapper.readTree(jsonStr);

            AtsAnalysisResponse response = AtsAnalysisResponse.builder()
                    .score(root.has("score") ? root.get("score").asInt() : 50)
                    .rating(root.has("rating") ? root.get("rating").asText() : "Moderate Compatibility")
                    .layoutSuggestions(jsonArrayToList(root, "layoutSuggestions"))
                    .keywordSuggestions(jsonArrayToList(root, "keywordSuggestions"))
                    .metricSuggestions(jsonArrayToList(root, "metricSuggestions"))
                    .verbSuggestions(jsonArrayToList(root, "verbSuggestions"))
                    .build();

            logger.info("Successfully parsed OpenAI response. Score: {}, Rating: {}", response.getScore(), response.getRating());
            return response;
        } catch (Exception e) {
            logger.error("AI ATS analysis call failed or failed to parse. Returning error fallback.", e);
            // If AI call fails, return a fallback
            return AtsAnalysisResponse.builder()
                    .score(50)
                    .rating("Moderate Compatibility")
                    .layoutSuggestions(List.of("⚠️ AI analysis encountered an error. Showing estimated results.", e.getMessage()))
                    .keywordSuggestions(List.of("⚠️ Retry the analysis to get AI-powered keyword feedback."))
                    .metricSuggestions(List.of("⚠️ Could not evaluate metrics via AI."))
                    .verbSuggestions(List.of("⚠️ Could not evaluate writing quality via AI."))
                    .build();
        }
    }

    private List<String> jsonArrayToList(JsonNode root, String fieldName) {
        List<String> result = new ArrayList<>();
        if (root.has(fieldName) && root.get(fieldName).isArray()) {
            for (JsonNode node : root.get(fieldName)) {
                result.add(node.asText());
            }
        }
        if (result.isEmpty()) {
            result.add("✅ No specific issues found in this category.");
        }
        return result;
    }

    /**
     * Mock ATS response based on simple heuristics when OpenAI API key is not configured.
     */
    private AtsAnalysisResponse buildMockAtsResponse(String resumeText) {
        String lower = resumeText.toLowerCase();
        int score = 40;
        List<String> layoutTips = new ArrayList<>();
        List<String> keywordTips = new ArrayList<>();
        List<String> metricTips = new ArrayList<>();
        List<String> verbTips = new ArrayList<>();

        // Basic content length scoring
        int wordCount = resumeText.split("\\s+").length;
        if (wordCount > 300) score += 15;
        else if (wordCount > 150) score += 8;
        else layoutTips.add("❌ Resume content is very sparse (" + wordCount + " words). Aim for 300-600 words.");

        // Check for common sections
        if (lower.contains("experience") || lower.contains("work history")) score += 8;
        else layoutTips.add("❌ No 'Experience' or 'Work History' section detected.");

        if (lower.contains("skills") || lower.contains("technologies")) score += 5;
        else keywordTips.add("❌ No 'Skills' or 'Technologies' section found.");

        if (lower.contains("education") || lower.contains("degree")) score += 5;
        else layoutTips.add("⚠️ No 'Education' section found.");

        if (lower.contains("project")) score += 5;

        // Check for metrics
        if (lower.matches(".*\\d+%.*") || lower.matches(".*\\$\\d+.*") || lower.contains("million") || lower.contains("thousand")) {
            score += 8;
            metricTips.add("✅ Quantified achievements detected (percentages, dollar amounts).");
        } else {
            metricTips.add("❌ No quantified metrics found. Add numbers like '30% improvement' or '$50K savings'.");
        }

        // Check for action verbs
        String[] verbs = {"architected", "engineered", "implemented", "developed", "optimized", "designed", "led", "managed", "deployed"};
        boolean hasVerbs = false;
        for (String v : verbs) {
            if (lower.contains(v)) { hasVerbs = true; break; }
        }
        if (hasVerbs) {
            score += 5;
            verbTips.add("✅ Strong action verbs detected in experience descriptions.");
        } else {
            verbTips.add("❌ Start bullet points with strong verbs: Architected, Engineered, Optimized, Led.");
        }

        // Contact info
        if (lower.contains("@") && lower.contains(".com")) score += 3;
        if (lower.contains("linkedin")) score += 2;
        if (lower.contains("github")) score += 2;

        score = Math.max(25, Math.min(score, 95));

        if (layoutTips.isEmpty()) layoutTips.add("✅ Resume structure looks organized.");
        if (keywordTips.isEmpty()) keywordTips.add("✅ Relevant technical keywords present.");
        if (metricTips.isEmpty()) metricTips.add("⚠️ Add more quantified metrics to stand out.");
        if (verbTips.isEmpty()) verbTips.add("✅ Writing quality is professional.");

        String rating;
        if (score >= 85) rating = "Highly ATS Compatible";
        else if (score >= 70) rating = "ATS-Friendly";
        else if (score >= 55) rating = "Moderate Compatibility";
        else rating = "Needs Significant Work";

        AtsAnalysisResponse response = AtsAnalysisResponse.builder()
                .score(score)
                .rating(rating)
                .layoutSuggestions(layoutTips)
                .keywordSuggestions(keywordTips)
                .metricSuggestions(metricTips)
                .verbSuggestions(verbTips)
                .build();

        logger.info("Generated offline mock ATS response. Score: {}, Rating: {}", response.getScore(), response.getRating());
        return response;
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

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("dummy") || apiKey.contains("please-set")) {
            logger.warn("OpenAI API key is not configured. Falling back to local offline dynamic experience optimizer.");
            return buildDynamicMockExperience(rawExperience);
        }

        String context = buildCandidateContext(username);

        String userPrompt = String.format("""
            Target Job Description:
            %s
            
            Raw Experience Text:
            %s
            %s
            """, jobDescription.trim(), rawExperience.trim(), context);

        logger.info("[OpenAI Request] Sending Experience Optimization request.\nSystem Prompt: {}\nUser Prompt: {}", SYSTEM_PROMPT, userPrompt);
        String rawResponse = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
        logger.info("[OpenAI Response] Received raw response from OpenAI: {}", rawResponse);
        return rawResponse;
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

    @CircuitBreaker(name = "openaiService", fallbackMethod = "handleOpenAiFallback")
    @Retry(name = "openaiService")
    public String startOptimizationTask(ExperienceOptimizationRequest request, String username) {
        if (request.getRawExperience() == null || request.getRawExperience().trim().isEmpty()) {
            throw new IllegalArgumentException("Raw experience text cannot be null or empty");
        }
        if (request.getTargetJobDescription() == null || request.getTargetJobDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Job description cannot be null or empty");
        }

        String taskId = UUID.randomUUID().toString();

        // Persist initial state in Redis
        AITaskState taskState = AITaskState.builder()
                .taskId(taskId)
                .username(username)
                .status("PROCESSING")
                .message("Task initiated")
                .timestamp(System.currentTimeMillis())
                .build();
        aiTaskStateRepository.save(taskState);

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
                    String mockResponse = buildDynamicMockExperience(request.getRawExperience());

                    String[] words = mockResponse.split(" ");
                    int count = 0;
                    for (String word : words) {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(word + " "));
                        Thread.sleep(85); // Simulate typing speed
                        count++;
                        if (count % 5 == 0) {
                            AITaskState current = aiTaskStateRepository.findById(taskId).orElse(taskState);
                            current.setMessage(String.format("Generating tokens: %d words emitted", count));
                            aiTaskStateRepository.save(current);
                        }
                    }

                    AITaskState finalState = aiTaskStateRepository.findById(taskId).orElse(taskState);
                    finalState.setStatus("COMPLETED");
                    finalState.setMessage("Task completed successfully");
                    aiTaskStateRepository.save(finalState);

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

                logger.info("[OpenAI Request] Sending Streaming Experience Optimization request.\nSystem Prompt: {}\nUser Prompt: {}", SYSTEM_PROMPT, userPrompt);
                Flux<String> contentFlux = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(userPrompt)
                        .stream()
                        .content();

                StringBuilder accumulatedResponse = new StringBuilder();
                int count = 0;
                for (String chunk : contentFlux.toIterable()) {
                    accumulatedResponse.append(chunk);
                    logger.info("[OpenAI Response Chunk] Received token: {}", chunk);
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(chunk));
                    count++;
                    if (count % 5 == 0) {
                        AITaskState current = aiTaskStateRepository.findById(taskId).orElse(taskState);
                        current.setMessage(String.format("Generating tokens: %d chunks emitted", count));
                        aiTaskStateRepository.save(current);
                    }
                }
                logger.info("[OpenAI Response] Stream Complete. Accumulated Response: {}", accumulatedResponse.toString());

                AITaskState finalState = aiTaskStateRepository.findById(taskId).orElse(taskState);
                finalState.setStatus("COMPLETED");
                finalState.setMessage("Task completed successfully");
                aiTaskStateRepository.save(finalState);

                // Send done event and complete
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(""));
                emitter.complete();
            } catch (Exception e) {
                AITaskState finalState = aiTaskStateRepository.findById(taskId).orElse(taskState);
                finalState.setStatus("FAILED");
                finalState.setMessage("Error: " + e.getMessage());
                aiTaskStateRepository.save(finalState);

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

    public String handleOpenAiFallback(ExperienceOptimizationRequest request, String username, Throwable t) {
        logger.error("AI Optimization service is temporarily degraded. Fallback triggered. Error: {}", t.getMessage());
        String taskId = UUID.randomUUID().toString();

        AITaskState taskState = AITaskState.builder()
                .taskId(taskId)
                .username(username)
                .status("PROCESSING")
                .message("Task initiated (fallback mode)")
                .timestamp(System.currentTimeMillis())
                .build();
        aiTaskStateRepository.save(taskState);

        SseEmitter emitter = new SseEmitter(180000L);
        emitterMap.put(taskId, emitter);

        executorService.submit(() -> {
            try {
                Thread.sleep(500);

                AITaskState finalState = aiTaskStateRepository.findById(taskId).orElse(taskState);
                finalState.setStatus("FAILED");
                finalState.setMessage("AI Optimization service is temporarily degraded. Retrying shortly.");
                aiTaskStateRepository.save(finalState);

                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("AI Optimization service is temporarily degraded. Retrying shortly."));
                emitter.complete();
            } catch (Exception ex) {
                logger.error("Failed to send fallback error event to emitter", ex);
                emitter.completeWithError(ex);
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

    private String buildMockSummaryResponse(SummaryGenerationRequest request) {
        StringBuilder sb = new StringBuilder("Results-driven professional ");
        if (request.getYearsOfExp() != null && !request.getYearsOfExp().trim().isEmpty()) {
            sb.append("with ").append(request.getYearsOfExp().trim()).append(" of experience ");
        }
        if (request.getSpecialization() != null && !request.getSpecialization().trim().isEmpty()) {
            sb.append("specializing in ").append(request.getSpecialization().trim()).append(". ");
        } else {
            sb.append("seeking to leverage technical capabilities. ");
        }
        if (request.getTargetRole() != null && !request.getTargetRole().trim().isEmpty()) {
            sb.append("Seeking a role as a ").append(request.getTargetRole().trim()).append(". ");
        }
        if (request.getPrimaryTechnologies() != null && !request.getPrimaryTechnologies().trim().isEmpty()) {
            sb.append("Proficient in ").append(request.getPrimaryTechnologies().trim()).append(". ");
        }
        sb.append("Demonstrated success designing and delivering optimized features while ensuring scalable software development.");
        return sb.toString();
    }

    private static final String SUMMARY_GENERATION_SYSTEM_PROMPT = """
        You are a professional resume writer and career coach.
        Your task is to write a highly polished, results-driven professional summary for a resume.
        
        You will be given the candidate's years of experience, area of specialization, target role, and primary technologies.
        
        Follow these instructions strictly:
        1. Write a single, cohesive paragraph of 3 to 4 sentences.
        2. Start with an impactful description of their experience and specialization (e.g., 'Results-driven [Target Role] with [Years of Experience] of expertise specializing in [Specialization]...').
        3. Highlight their key skills and technologies naturally in the middle sentence.
        4. End with a strong statement about their value proposition and what they aim to achieve in their next role.
        5. Keep the tone professional, active, and impact-driven.
        6. Do NOT include any formatting, markdown, quotes, bullet points, introductory or concluding remarks. Output ONLY the raw professional summary paragraph.
        """;

    public String generateProfessionalSummary(SummaryGenerationRequest request) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("dummy") || apiKey.contains("please-set")) {
            logger.warn("OpenAI API key is not configured. Falling back to local offline heuristic summary generator.");
            return buildMockSummaryResponse(request);
        }

        String userPrompt = String.format(
            "Candidate details:\n- Years of Experience: %s\n- Specialization: %s\n- Target Role: %s\n- Primary Technologies: %s",
            request.getYearsOfExp() != null ? request.getYearsOfExp() : "",
            request.getSpecialization() != null ? request.getSpecialization() : "",
            request.getTargetRole() != null ? request.getTargetRole() : "",
            request.getPrimaryTechnologies() != null ? request.getPrimaryTechnologies() : ""
        );

        try {
            logger.info("[OpenAI Request] Sending summary generation request.\nSystem Prompt: {}\nUser Prompt: {}", SUMMARY_GENERATION_SYSTEM_PROMPT, userPrompt);
            String rawResponse = chatClient.prompt()
                    .system(SUMMARY_GENERATION_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            logger.info("[OpenAI Response] Received summary from OpenAI: {}", rawResponse);
            return rawResponse.trim();
        } catch (Exception e) {
            logger.error("OpenAI summary generation failed. Falling back to mock summary response.", e);
            return buildMockSummaryResponse(request);
        }
    }

    private String buildDynamicMockExperience(String rawExperience) {
        if (rawExperience == null || rawExperience.trim().isEmpty()) {
            return "";
        }
        String[] lines = rawExperience.split("\n");
        StringBuilder sb = new StringBuilder();
        String[] verbs = {"Optimized", "Engineered", "Implemented", "Developed", "Led", "Architected", "Spearheaded"};
        int verbIndex = 0;
        for (String line : lines) {
            String clean = line.trim();
            if (clean.isEmpty()) continue;
            // Remove existing bullet characters
            clean = clean.replaceAll("^\\s*[•\\-*#+]\\s*", "");
            if (clean.isEmpty()) continue;
            
            // Check if it already starts with a verb or capitalize first word
            String[] words = clean.split("\\s+");
            if (words.length > 0) {
                String firstWord = words[0];
                if (!firstWord.toLowerCase().endsWith("ed") && !firstWord.toLowerCase().endsWith("ing")) {
                    String chosenVerb = verbs[verbIndex % verbs.length];
                    verbIndex++;
                    clean = chosenVerb + " " + Character.toLowerCase(clean.charAt(0)) + clean.substring(1);
                } else {
                    clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
                }
            }
            sb.append("• ").append(clean).append("\n");
        }
        return sb.toString().trim();
    }
}
