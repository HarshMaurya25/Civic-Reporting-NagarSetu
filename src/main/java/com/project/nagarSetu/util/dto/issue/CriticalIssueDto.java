package com.project.nagarSetu.util.dto.issue;

import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalIssueDto {
    private UUID id;
    private String title;
    private Criticality criticality;
    private int upvoteCount;
    private LocalDateTime createdAt;
    private Stages stage;
    private double priorityScore;
}
