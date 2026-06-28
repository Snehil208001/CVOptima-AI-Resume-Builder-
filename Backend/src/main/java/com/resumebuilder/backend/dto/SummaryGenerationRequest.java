package com.resumebuilder.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryGenerationRequest {
    private String yearsOfExp;
    private String specialization;
    private String targetRole;
    private String primaryTechnologies;
}
