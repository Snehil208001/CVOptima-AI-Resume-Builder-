package com.resumebuilder.backend.repository;

import com.resumebuilder.backend.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    
    List<Experience> findByResumeId(Long resumeId);
}
