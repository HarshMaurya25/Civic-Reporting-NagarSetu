package com.project.nagarSetu.util.dto.issue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueMatrixBucketDto {
    private String period;
    private LocalDateTime from;
    private LocalDateTime to;
    private long reported;
    private long solved;
    private long inBetween;
}