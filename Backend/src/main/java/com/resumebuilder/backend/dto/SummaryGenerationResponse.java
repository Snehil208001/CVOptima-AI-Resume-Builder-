package com.resumebuilder.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryGenerationResponse {
    private String summary;
}
