package com.resumebuilder.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "resume")
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current_role")
    private Boolean isCurrentRole;

    @Column(name = "location")
    private String location;

    @Column(name = "type")
    private String type; // e.g. "Remote", "On-site", "Hybrid"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "experience_bullets", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "bullet_point", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> bulletPoints = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Experience that = (Experience) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
