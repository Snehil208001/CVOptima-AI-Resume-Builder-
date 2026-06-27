package com.resumebuilder.backend.repository;

import com.resumebuilder.backend.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByResumeId(Long resumeId);
}
