package com.project.nagarSetu.controller;

import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.service.admin.AdminService;
import com.project.nagarSetu.util.dto.admin.AdminWorkerDto;
import com.project.nagarSetu.util.dto.worker.WorkerCreateResponse;
import com.project.nagarSetu.util.dto.worker.WorkerLoginReponseDto;
import com.project.nagarSetu.util.dto.issue.IssueMatrixSummaryDto;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import com.project.nagarSetu.util.dto.admin.AdminUserDto;
import com.project.nagarSetu.util.dto.admin.AdminStatsOverviewDto;
import com.project.nagarSetu.util.dto.admin.UserBasicDetailDto;
import com.project.nagarSetu.util.dto.admin.WardDetailDto;
import com.project.nagarSetu.util.dto.issue.IssueStageMatrixDto;
import com.project.nagarSetu.util.dto.issue.WardMatrixRowDto;

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
    @GetMapping("/wards")
    public ResponseEntity<List<com.project.nagarSetu.util.dto.admin.AdminWardDto>> getAllWards() {
        return ResponseEntity.ok(adminService.getAllAdminWards());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/wards/detail")
    public ResponseEntity<WardDetailDto> getWardDetail(
            @RequestParam(required = false) UUID wardId,
            @RequestParam(required = false) String wardName) {
        return ResponseEntity.ok(adminService.getWardDetail(wardId, wardName));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/wards/{wardId}/supervisor/{supervisorId}")
    public ResponseEntity<Boolean> allocateWardToSupervisor(
            @PathVariable UUID wardId,
            @PathVariable UUID supervisorId) {
        return ResponseEntity.ok(adminService.allocateWardToSupervisor(wardId, supervisorId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/supervisors/{supervisorId}")
    public ResponseEntity<Boolean> updateSupervisor(
            @PathVariable UUID supervisorId,
            @RequestBody com.project.nagarSetu.util.dto.admin.UpdateSupervisorDto request) {
        return ResponseEntity.ok(adminService.updateSupervisor(supervisorId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/supervisors/{supervisorId}")
    public ResponseEntity<Boolean> deleteSupervisor(@PathVariable UUID supervisorId) {
        return ResponseEntity.ok(adminService.deleteSupervisor(supervisorId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/workers")
    public ResponseEntity<List<AdminUserDto>> getAllWorkers() {
        return new ResponseEntity<>(adminService.getAllWorkers(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/workers/simple")
    public ResponseEntity<List<com.project.nagarSetu.util.dto.admin.SimpleWorkerDto>> getAllWorkersSimple() {
        return new ResponseEntity<>(adminService.getAllWorkersSimple(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/workers/debug")
    public ResponseEntity<List<Worker>> getAllWorkersDebug() {
        return new ResponseEntity<>(adminService.getAllWorkersRaw(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/supervisors/{supervisorId}/detail")
    public ResponseEntity<UserBasicDetailDto> getSupervisorDetail(@PathVariable UUID supervisorId) {
        return ResponseEntity.ok(adminService.getSupervisorDetail(supervisorId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/workers/{workerId}/detail")
    public ResponseEntity<UserBasicDetailDto> getWorkerDetail(@PathVariable UUID workerId) {
        return ResponseEntity.ok(adminService.getWorkerDetail(workerId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/citizens/{userId}/detail")
    public ResponseEntity<UserBasicDetailDto> getCitizenDetail(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getCitizenDetail(userId));
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

    @GetMapping("/issues/stats/matrix")
    public ResponseEntity<IssueMatrixSummaryDto> getIssueMatrixForAdmin(
            @RequestParam(required = false) UUID wardId) {
        return ResponseEntity.ok(adminService.getIssueMatrixSummary(wardId));
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<AdminStatsOverviewDto> getAdminOverviewStats() {
        return ResponseEntity.ok(adminService.getAdminOverviewStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issues/stats/matrix/stages")
    public ResponseEntity<List<IssueStageMatrixDto>> getStageMatrix(
            @RequestParam(required = false) UUID wardId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(adminService.getStageMatrix(wardId, days));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issues/stats/matrix/wards")
    public ResponseEntity<List<WardMatrixRowDto>> getWardMatrix(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getWardMatrix(days));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issues/stats/matrix/sla-breaches")
    public ResponseEntity<Long> getSlaBreaches(
            @RequestParam(required = false) UUID wardId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getSlaBreachedCount(wardId, days));
    }

    @GetMapping("/wards/{wardId}/issues/stats/matrix")
    public ResponseEntity<IssueMatrixSummaryDto> getIssueMatrixForWard(@PathVariable UUID wardId) {
        return ResponseEntity.ok(adminService.getIssueMatrixSummary(wardId));
    }

}
