package com.project.nagarSetu.controller;

import com.project.nagarSetu.service.admin.AdminService;
import com.project.nagarSetu.util.dto.admin.AdminWorkerDto;
import com.project.nagarSetu.util.dto.worker.WorkerCreateResponse;
import com.project.nagarSetu.util.dto.worker.WorkerLoginReponseDto;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import com.project.nagarSetu.util.dto.admin.AdminUserDto;

@RestController
@AllArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/acceptWorker/{workerId}/{supervisorId}")
    public ResponseEntity<Boolean> acceptWorker(
            @PathVariable UUID workerId,
            @PathVariable UUID supervisorId) {
        adminService.acceptWorker(workerId, supervisorId);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reassignWorker/{workerId}/{supervisorId}")
    public ResponseEntity<Boolean> reassignWorker(
            @PathVariable UUID workerId,
            @PathVariable UUID supervisorId) {
        adminService.reassignWorker(workerId, supervisorId);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reassignIssueWorker/{issueId}/{workerId}")
    public ResponseEntity<Boolean> reassignIssueWorker(
            @PathVariable UUID issueId,
            @PathVariable UUID workerId) {
        adminService.reassignIssueWorker(issueId, workerId);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/acceptSupervisor/{supervisorId}")
    public ResponseEntity<Boolean> acceptSupervisor(
            @PathVariable UUID supervisorId,
            @RequestBody com.project.nagarSetu.util.dto.admin.AcceptSupervisorDto request) {
        adminService.acceptSuperVisior(supervisorId, request);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/workers")
    public ResponseEntity<List<AdminUserDto>> getAllWorkers() {
        return new ResponseEntity<>(adminService.getAllWorkers(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/supervisors")
    public ResponseEntity<List<AdminWorkerDto>> getAllSupervisors() {
        return new ResponseEntity<>(adminService.getAllSupervisors(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/supervisors/no-start")
    public ResponseEntity<List<AdminWorkerDto>> getAllSupervisorsWithNoStart() {
        return ResponseEntity.ok(adminService.getAllSupervisorsWithNoStart());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/workers/no-start")
    public ResponseEntity<List<AdminUserDto>> getAllWorkersWithNoStarted() {
        return ResponseEntity.ok(adminService.getAllWorkersWithNoStarted());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issues/stats/30days")
    public ResponseEntity<com.project.nagarSetu.util.dto.issue.Issue30DayStatsDto> getIssue30DayStats() {
        com.project.nagarSetu.util.dto.issue.Issue30DayStatsDto stats = adminService.getIssue30DayStats();
        if (stats == null)
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(stats);
    }

}
