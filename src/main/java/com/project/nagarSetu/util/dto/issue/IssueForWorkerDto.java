package com.project.nagarSetu.util.dto.issue;

import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.IssueType;
import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class IssueForWorkerDto {
    private UUID id;
    private String title;
    private String description;
    private Stages stages;
    private LocalDateTime createAt;
    private String secureURL;
    private Criticality criticality;
    private IssueType issueType;
    private String supervisorName;
}
