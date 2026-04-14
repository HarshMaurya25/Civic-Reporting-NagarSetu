package com.project.nagarSetu.util.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsOverviewDto {
    private Long wardCount;
    private Long monthlyReportedIssues;
    private Long monthlySolvedIssues;
    private Long workerCount;
    private Long supervisorCount;
    private Long totalIssuesReported;
    private Long totalIssuesResolved;
}
