package com.resumebuilder.backend.repository;

import com.resumebuilder.backend.entity.AITaskState;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiTaskStateRepository extends CrudRepository<AITaskState, String> {
}
