package com.project.nagarSetu.util.dto.issue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueMatrixSummaryDto {
    private UUID wardId;
    private IssueMatrixBucketDto daily;
    private IssueMatrixBucketDto weekly;
    private IssueMatrixBucketDto monthly;
}