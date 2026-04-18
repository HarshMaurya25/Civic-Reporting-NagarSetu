package com.project.nagarSetu.service.analytics;

import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.repository.IssueRepository;
import com.project.nagarSetu.util.dto.issue.CriticalIssueDto;
import com.project.nagarSetu.util.dto.issue.IssueDailyCountDto;
import com.project.nagarSetu.util.dto.issue.IssueTimeSlotDto;
import com.project.nagarSetu.util.enums.Criticality;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final IssueRepository issueRepository;

    public List<IssueTimeSlotDto> getTimeWiseAnalytics(UUID wardId) {
        String wardIdStr = wardId != null ? wardId.toString() : null;
        List<Object[]> results = issueRepository.getHourlyCounts(wardIdStr);
        
        long[] slots = new long[6]; // 0-4, 4-8, 8-12, 12-16, 16-20, 20-24
        
        for (Object[] row : results) {
            int hour = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            slots[hour / 4] += count;
        }

        String[] labels = {
            "00:00 - 04:00 (Night)",
            "04:00 - 08:00 (Early Morning)",
            "08:00 - 12:00 (Morning)",
            "12:00 - 16:00 (Noon)",
            "16:00 - 20:00 (Evening)",
            "20:00 - 00:00 (Late Night)"
        };

        List<IssueTimeSlotDto> dtos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            dtos.add(new IssueTimeSlotDto(labels[i], slots[i]));
        }
        return dtos;
    }

    public List<IssueDailyCountDto> getDayWiseAnalytics(UUID wardId, int days) {
        String wardIdStr = wardId != null ? wardId.toString() : null;
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        // Use existing methods if available or repurpose getDailyCountsForWardSince
        List<Object[]> results;
        if (wardIdStr != null) {
            results = issueRepository.getDailyCountsForWardSince(wardIdStr, since);
        } else {
            // Reusing the general one from IssueRepository
            results = issueRepository.getCreatedCountSinceGroupedByDate(since);
        }

        return results.stream()
                .map(row -> new IssueDailyCountDto(row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public List<CriticalIssueDto> getCriticalIssues(UUID wardId, int limit) {
        String wardIdStr = wardId != null ? wardId.toString() : null;
        List<Issue> unresolved = issueRepository.findUnresolvedByWardOrOverall(wardIdStr);

        return unresolved.stream()
                .map(this::mapToCriticalDto)
                .sorted(Comparator.comparingDouble(CriticalIssueDto::getPriorityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<IssueDailyCountDto> getYearlyWardGraph(UUID wardId) {
        if (wardId == null) return Collections.emptyList();
        LocalDateTime since = LocalDateTime.now().minusYears(1);
        List<Object[]> results = issueRepository.getDailyCountsForWardSince(wardId.toString(), since);

        return results.stream()
                .map(row -> new IssueDailyCountDto(row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    private CriticalIssueDto mapToCriticalDto(Issue issue) {
        double score = calculatePriorityScore(issue);
        return CriticalIssueDto.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .criticality(issue.getCriticality())
                .upvoteCount(issue.getUpvoteCount() != null ? issue.getUpvoteCount() : 0)
                .createdAt(issue.getCreateAt())
                .stage(issue.getStages())
                .priorityScore(score)
                .build();
    }

    private double calculatePriorityScore(Issue issue) {
        double criticalityWeight = 0;
        if (issue.getCriticality() == Criticality.HIGH) criticalityWeight = 3.0;
        else if (issue.getCriticality() == Criticality.MEDIUM) criticalityWeight = 2.0;
        else criticalityWeight = 1.0;

        int upvotes = issue.getUpvoteCount() != null ? issue.getUpvoteCount() : 0;
        
        long daysOld = ChronoUnit.DAYS.between(issue.getCreateAt(), LocalDateTime.now());
        if (daysOld < 0) daysOld = 0;

        // Formula: (Criticality * 100) + (Upvotes * 10) + (AgeInDays * 5)
        return (criticalityWeight * 100) + (upvotes * 10) + (daysOld * 5);
    }
}
