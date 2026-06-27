package com.resumebuilder.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "layout_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "resume")
public class LayoutConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "layout_density")
    private String layoutDensity; // "Compact", "Normal", "Spacious"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "layout_section_order", joinColumns = @JoinColumn(name = "layout_config_id"))
    @Column(name = "section_name")
    @OrderColumn(name = "section_index")
    @Builder.Default
    private List<String> sectionOrder = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayoutConfig that = (LayoutConfig) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
