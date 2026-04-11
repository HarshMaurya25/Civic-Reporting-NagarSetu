package com.project.nagarSetu.util.dto.issue;


import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.IssueType;
import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueByMap {
    private UUID id;

    private double latitude;

    private double longitude;

    private Criticality criticality;

    private Stages stages;

    private IssueType issueType;

}
