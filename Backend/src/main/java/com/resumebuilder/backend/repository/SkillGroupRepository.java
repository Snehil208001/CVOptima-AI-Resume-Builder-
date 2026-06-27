package com.resumebuilder.backend.repository;

import com.resumebuilder.backend.entity.SkillGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillGroupRepository extends JpaRepository<SkillGroup, Long> {
    List<SkillGroup> findByResumeId(Long resumeId);
}
