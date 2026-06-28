package com.resumebuilder.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtsAnalysisRequest {
    private String resumeText;
    private String targetJobDescription;
}
