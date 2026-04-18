package com.project.nagarSetu.controller.analytics;

import com.project.nagarSetu.service.analytics.AnalyticsService;
import com.project.nagarSetu.util.dto.issue.CriticalIssueDto;
import com.project.nagarSetu.util.dto.issue.IssueDailyCountDto;
import com.project.nagarSetu.util.dto.issue.IssueTimeSlotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/issues")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. Time wise Issue graph (Overall)
    @GetMapping("/time-wise")
    public ResponseEntity<List<IssueTimeSlotDto>> getTimeWiseOverall() {
        return ResponseEntity.ok(analyticsService.getTimeWiseAnalytics(null));
    }

    // 2. Time wise Issue graph (Ward wise)
    @GetMapping("/time-wise/{wardId}")
    public ResponseEntity<List<IssueTimeSlotDto>> getTimeWiseByWard(@PathVariable UUID wardId) {
        return ResponseEntity.ok(analyticsService.getTimeWiseAnalytics(wardId));
    }

    // 3. Day wise issue graph (Overall)
    @GetMapping("/day-wise")
    public ResponseEntity<List<IssueDailyCountDto>> getDayWiseOverall(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getDayWiseAnalytics(null, days));
    }

    // 4. Day wise issue graph (Ward wise)
    @GetMapping("/day-wise/{wardId}")
    public ResponseEntity<List<IssueDailyCountDto>> getDayWiseByWard(@PathVariable UUID wardId, @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getDayWiseAnalytics(wardId, days));
    }

    // 5. Most critical issues detector (Overall)
    @GetMapping("/critical")
    public ResponseEntity<List<CriticalIssueDto>> getCriticalIssuesOverall(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getCriticalIssues(null, limit));
    }

    // 6. Most critical issues detector (Ward wise)
    @GetMapping("/critical/{wardId}")
    public ResponseEntity<List<CriticalIssueDto>> getCriticalIssuesByWard(@PathVariable UUID wardId, @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getCriticalIssues(wardId, limit));
    }

    // 7. Make number of issue per day for a year graph for ward
    @GetMapping("/year-graph/{wardId}")
    public ResponseEntity<List<IssueDailyCountDto>> getYearlyWardGraph(@PathVariable UUID wardId) {
        return ResponseEntity.ok(analyticsService.getYearlyWardGraph(wardId));
    }
}
