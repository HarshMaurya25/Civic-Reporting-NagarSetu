package com.project.nagarSetu.util.dto.issue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkerMatrixSummaryDto {
    private UUID workerId;
    private IssueMatrixBucketDto daily;
    private IssueMatrixBucketDto weekly;
    private IssueMatrixBucketDto monthly;
}

