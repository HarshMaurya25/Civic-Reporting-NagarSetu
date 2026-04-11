package com.project.nagarSetu.controller;

import com.project.nagarSetu.error.exception.AccessDeniedUserException;
import com.project.nagarSetu.service.authenication.UserDetail;
import com.project.nagarSetu.service.issue.IssueService;
import com.project.nagarSetu.util.dto.issue.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/issue")
public class IssueController {

    private final IssueService issueService;

    @PostMapping("/create")
    public ResponseEntity<UUID> createIssue(
            @Validated @RequestPart IssueCreateDto issueCreateDto,
            @RequestPart MultipartFile image) {
        UUID issueID = issueService.createIssue(issueCreateDto, image);
        return new ResponseEntity<>(issueID, HttpStatus.CREATED);
    }

    @PostMapping("/done")
    public ResponseEntity<UUID> doneIssue(
            @RequestParam UUID id,
            @RequestPart("file") MultipartFile file) {
        return new ResponseEntity<>(issueService.doneIssue(id, file), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueGetDto> getIssue(@PathVariable UUID id) {
        IssueGetDto issue = issueService.getIssue(id);
        return new ResponseEntity<>(issue, HttpStatus.OK);

    }

    @GetMapping("/user")
    public ResponseEntity<Page<IssueGetByUserDto>> getIssueByUserId(
            @RequestParam UUID id,
            @RequestParam int pageNumber) {
        Page<IssueGetByUserDto> issues = issueService.getIssueByUserId(id, pageNumber);
        return new ResponseEntity<>(issues, HttpStatus.OK);
    }

    @GetMapping("/user/map")
    public ResponseEntity<Set<IssueByMap>> getIssueMapByUser(@RequestParam UUID id) {
        Set<IssueByMap> issueMap = issueService.getIssueMapByUser(id);

        if (issueMap.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(issueMap, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/map/supervisor")
    public ResponseEntity<Set<IssueByMap>> getIssueMapForSupervisor(@RequestParam UUID supervisorId) {
        Set<IssueByMap> issueMap = issueService.getIssueMapForSupervisor(supervisorId);
        if (issueMap == null || issueMap.isEmpty())
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(issueMap);
    }

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/map/worker")
    public ResponseEntity<Set<IssueByMap>> getIssueMapForWorker(@RequestParam UUID workerId) {
        Set<IssueByMap> issueMap = issueService.getIssueMapForWorker(workerId);
        if (issueMap == null || issueMap.isEmpty())
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(issueMap);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/map/admin")
    public ResponseEntity<Set<IssueByMap>> getIssueMapForAdmin() {
        Set<IssueByMap> issueMap = issueService.getIssueMapForAdmin();
        if (issueMap == null || issueMap.isEmpty())
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(issueMap);
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<IssueRecent>> getRecentIssues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<IssueRecent> recentIssues = issueService.getRecentIssue(page, size);
        return ResponseEntity.ok(recentIssues);
    }

    @GetMapping("/stats/weekly/stages")
    public ResponseEntity<java.util.List<com.project.nagarSetu.util.dto.issue.IssueStageCountDto>> getWeeklyStageCounts() {
        java.util.List<com.project.nagarSetu.util.dto.issue.IssueStageCountDto> stats = issueService
                .getWeeklyStageCounts();
        if (stats == null || stats.isEmpty())
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{issueId}/worker")
    public ResponseEntity<GetIssueWorkker> getIssueWorker(
            @PathVariable UUID issueId
    ) {
        return ResponseEntity.ok(issueService.getIssueWorker(issueId));
    }

    @PutMapping("/{issueId}/reassign/{workerId}")
    public ResponseEntity<Void> reassignIssue(
            @PathVariable UUID issueId,
            @PathVariable UUID workerId
    ) {
        issueService.reassignIssue(issueId, workerId);
        return ResponseEntity.ok().build();
    }

}
