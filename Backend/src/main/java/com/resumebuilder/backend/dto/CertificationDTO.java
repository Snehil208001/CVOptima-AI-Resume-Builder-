package com.resumebuilder.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationDTO {
    private Long id;

    @NotBlank(message = "Certification title is required")
    private String title;

    private String issuer;
    private String link;
    private LocalDate date;
    private List<String> bulletPoints;
}
