package com.project.nagarSetu.util.dto.issue;

import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueStageMatrixDto {
    private Stages stage;
    private long count;
}

