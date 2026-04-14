package com.project.nagarSetu.util.dto.issue;

import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.IssueType;
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
public class IssueSolvedDto {
    private UUID id;
    private String title;
    private IssueType issueType;
    private Criticality criticality;
    private String location;
    private Stages stages;
    private String submittedBy;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String issueImageUrl;
    private String resolvedImageUrl;
}