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
public class ExperienceDTO {

    private Long id;

    @NotBlank(message = "Company is required")
    private String company;

    @NotBlank(message = "Title is required")
    private String title;

    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrentRole;
    private String location;
    private String type;
    private List<String> bulletPoints;
    private String description;
}
