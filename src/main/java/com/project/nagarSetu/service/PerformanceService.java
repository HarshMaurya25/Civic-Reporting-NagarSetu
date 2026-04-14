package com.project.nagarSetu.service;

import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.repository.IssueRepository;
import com.project.nagarSetu.util.dto.WardPerformanceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerformanceService {

    @Autowired
    private IssueRepository issueRepository;

    public List<WardPerformanceDto> getTopPerformingWards() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<Issue> issues = issueRepository.findByCreateAtAfter(oneYearAgo);

        return issues.stream()
                .collect(Collectors.groupingBy(Issue::getWardId))
                .entrySet().stream()
                .map(entry -> {
                    String wardId = entry.getKey();
                    List<Issue> wardIssues = entry.getValue();
                    long totalIssues = wardIssues.size();
                    long resolvedIssues = wardIssues.stream().filter(issue -> issue.getResolvedAt() != null).count();
                    double resolutionRate = totalIssues == 0 ? 0 : (double) resolvedIssues / totalIssues;

                    long totalResolutionTime = wardIssues.stream()
                            .filter(issue -> issue.getResolvedAt() != null)
                            .mapToLong(issue -> java.time.Duration.between(issue.getCreateAt(), issue.getResolvedAt())
                                    .toHours())
                            .sum();
                    double avgResolutionTime = resolvedIssues == 0 ? 0 : (double) totalResolutionTime / resolvedIssues;

                    long slaBreaches = wardIssues.stream().filter(Issue::isSlaBreached).count();

                    long criticalIssuesResolved = wardIssues.stream()
                            .filter(issue -> issue.getResolvedAt() != null
                                    && issue.getCriticality() == com.project.nagarSetu.util.enums.Criticality.HIGH)
                            .count();

                    long reopenedIssues = wardIssues.stream().mapToInt(Issue::getReopenCount).sum();

                    double feedbackScore = wardIssues.stream()
                            .filter(issue -> issue.getCitizenFeedbackScore() != null)
                            .mapToInt(Issue::getCitizenFeedbackScore)
                            .average()
                            .orElse(0);

                    // Scoring logic
                    double points = (resolutionRate * 50)
                            - (avgResolutionTime * 0.1)
                            - (slaBreaches * 10)
                            + (criticalIssuesResolved * 5)
                            - (reopenedIssues * 5)
                            + (feedbackScore * 2.5);

                    String supervisorName = wardIssues.stream()
                            .map(Issue::getSupervisior)
                            .filter(supervisior -> supervisior != null && supervisior.getUser() != null)
                            .map(supervisior -> supervisior.getUser().getFullName())
                            .filter(name -> name != null && !name.isBlank())
                            .findFirst()
                            .orElse("N/A");

                    return new WardPerformanceDto(wardId, supervisorName, points);
                })
                .sorted((a, b) -> Double.compare(b.getPoints(), a.getPoints()))
                .limit(10)
                .collect(Collectors.toList());
    }
}
