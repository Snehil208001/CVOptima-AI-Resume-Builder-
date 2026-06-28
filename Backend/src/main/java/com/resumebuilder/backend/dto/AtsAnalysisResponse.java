package com.resumebuilder.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtsAnalysisResponse {
    private int score;
    private String rating;
    private List<String> layoutSuggestions;
    private List<String> keywordSuggestions;
    private List<String> metricSuggestions;
    private List<String> verbSuggestions;
}
