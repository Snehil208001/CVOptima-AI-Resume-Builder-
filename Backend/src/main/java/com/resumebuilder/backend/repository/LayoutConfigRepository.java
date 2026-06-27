package com.resumebuilder.backend.repository;

import com.resumebuilder.backend.entity.LayoutConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LayoutConfigRepository extends JpaRepository<LayoutConfig, Long> {
    Optional<LayoutConfig> findByResumeId(Long resumeId);
}
