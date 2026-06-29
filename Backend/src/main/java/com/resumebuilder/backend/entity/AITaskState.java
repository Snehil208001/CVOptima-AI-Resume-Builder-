package com.resumebuilder.backend.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash("ai_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AITaskState implements Serializable {

    @Id
    private String taskId;
    private String username;
    private String status;
    private String message;
    private long timestamp;
}
