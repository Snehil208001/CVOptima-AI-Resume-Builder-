package com.resumebuilder.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "experiences", "educations", "skillGroups", "projects", "certifications", "layoutConfig"})
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Experience> experiences = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> educations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkillGroup> skillGroups = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();

    @OneToOne(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private LayoutConfig layoutConfig;

    // Bi-directional Experience helper methods
    public void addExperience(Experience experience) {
        experiences.add(experience);
        experience.setResume(this);
    }

    public void removeExperience(Experience experience) {
        experiences.remove(experience);
        experience.setResume(null);
    }

    // Bi-directional Education helper methods
    public void addEducation(Education education) {
        educations.add(education);
        education.setResume(this);
    }

    public void removeEducation(Education education) {
        educations.remove(education);
        education.setResume(null);
    }

    // Bi-directional SkillGroup helper methods
    public void addSkillGroup(SkillGroup skillGroup) {
        skillGroups.add(skillGroup);
        skillGroup.setResume(this);
    }

    public void removeSkillGroup(SkillGroup skillGroup) {
        skillGroups.remove(skillGroup);
        skillGroup.setResume(null);
    }

    // Bi-directional Project helper methods
    public void addProject(Project project) {
        projects.add(project);
        project.setResume(this);
    }

    public void removeProject(Project project) {
        projects.remove(project);
        project.setResume(null);
    }

    // Bi-directional Certification helper methods
    public void addCertification(Certification certification) {
        certifications.add(certification);
        certification.setResume(this);
    }

    public void removeCertification(Certification certification) {
        certifications.remove(certification);
        certification.setResume(null);
    }

    // LayoutConfig helper methods
    public void setLayoutConfig(LayoutConfig layoutConfig) {
        this.layoutConfig = layoutConfig;
        if (layoutConfig != null) {
            layoutConfig.setResume(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resume resume = (Resume) o;
        return id != null && Objects.equals(id, resume.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
