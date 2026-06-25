package com.resumebuilder.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceOptimizationRequest {

    @NotBlank(message = "Raw experience text is required")
    private String rawExperience;

    @NotBlank(message = "Target job description is required")
    private String targetJobDescription;
}
