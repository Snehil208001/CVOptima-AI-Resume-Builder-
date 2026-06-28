package com.resumebuilder.backend.controller;

import com.resumebuilder.backend.dto.AtsAnalysisRequest;
import com.resumebuilder.backend.dto.AtsAnalysisResponse;
import com.resumebuilder.backend.dto.ExperienceOptimizationRequest;
import com.resumebuilder.backend.dto.ExperienceOptimizationResponse;
import com.resumebuilder.backend.dto.SummaryGenerationRequest;
import com.resumebuilder.backend.dto.SummaryGenerationResponse;
import com.resumebuilder.backend.service.AiResumeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/ai")
public class AiResumeController {

    private final AiResumeService aiResumeService;

    public AiResumeController(AiResumeService aiResumeService) {
        this.aiResumeService = aiResumeService;
    }

    @PostMapping("/optimize")
    public ResponseEntity<ExperienceOptimizationResponse> optimizeExperience(
            java.security.Principal principal,
            @Valid @RequestBody ExperienceOptimizationRequest request) {
        
        String username = principal != null ? principal.getName() : null;
        String taskId = aiResumeService.startOptimizationTask(request, username);

        ExperienceOptimizationResponse response = ExperienceOptimizationResponse.builder()
                .taskId(taskId)
                .build();

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/optimize/{taskId}/stream")
    public SseEmitter streamOptimization(@PathVariable String taskId) {
        SseEmitter emitter = aiResumeService.getEmitter(taskId);
        if (emitter == null) {
            throw new IllegalArgumentException("Task ID not found or already completed");
        }
        return emitter;
    }

    @PostMapping("/ats-analyze")
    public ResponseEntity<AtsAnalysisResponse> analyzeAts(
            @RequestBody AtsAnalysisRequest request) {
        AtsAnalysisResponse response = aiResumeService.analyzeAtsScore(
                request.getResumeText(),
                request.getTargetJobDescription()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/summary")
    public ResponseEntity<SummaryGenerationResponse> generateSummary(
            @RequestBody SummaryGenerationRequest request) {
        String summary = aiResumeService.generateProfessionalSummary(request);
        return ResponseEntity.ok(new SummaryGenerationResponse(summary));
    }
}
