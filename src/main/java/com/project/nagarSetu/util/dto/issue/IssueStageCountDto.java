package com.project.nagarSetu.util.dto.issue;

import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IssueStageCountDto {
    private Stages stage;
    private long count;
}