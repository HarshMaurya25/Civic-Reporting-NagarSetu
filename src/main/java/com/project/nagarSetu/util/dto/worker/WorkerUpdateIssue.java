package com.project.nagarSetu.util.dto.worker;

import com.project.nagarSetu.util.enums.Stages;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkerUpdateIssue {
    private UUID workerId;

    private UUID issueId;

    private Stages stages;

    private String description;
}
