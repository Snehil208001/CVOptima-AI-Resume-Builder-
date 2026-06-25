package com.resumebuilder.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDTO {

    private Long id;

    @NotBlank(message = "Skill name is required")
    private String name;

    private String proficiencyLevel;
}
