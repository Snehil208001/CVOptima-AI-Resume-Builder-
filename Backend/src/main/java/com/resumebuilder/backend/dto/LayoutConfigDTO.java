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
public class LayoutConfigDTO {
    private Long id;
    private String layoutDensity;
    private List<String> sectionOrder;
}
