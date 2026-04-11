package com.project.nagarSetu.util.dto.issue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerScoringDto {
    private UUID workerId;
    private String fullName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime lastAssignedAt;
    private Integer lifetimeAssignments;
    private Long activeIssueCount;
}
