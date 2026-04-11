package com.project.nagarSetu.controller;

import com.project.nagarSetu.service.worker.WorkerService;
import com.project.nagarSetu.util.dto.authentication.LoginRequestDto;
import com.project.nagarSetu.util.dto.authentication.RegistrationRequestDto;
import com.project.nagarSetu.util.dto.issue.GetIssueWorkker;
import com.project.nagarSetu.util.dto.worker.WorkerCreateResponse;
import com.project.nagarSetu.util.dto.worker.WorkerLoginReponseDto;
import com.project.nagarSetu.util.dto.worker.WorkerUpdateIssue;
import com.project.nagarSetu.util.enums.Stages;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/worker")
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping("/registration")
    public ResponseEntity<WorkerCreateResponse> registration(
            @Validated @RequestBody RegistrationRequestDto requestDto) {
        WorkerCreateResponse response = workerService.getRegisterWorker(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<WorkerLoginReponseDto> login(
            @Validated @RequestBody LoginRequestDto requestDto) {
        return new ResponseEntity<>(workerService.getLogin(requestDto), HttpStatus.OK);
    }

    @GetMapping("/getCode")
    public ResponseEntity<Void> getCode(@RequestParam String email, @RequestParam String roles) {
        workerService.getOTP(email, roles);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/issues/assigned")
    public ResponseEntity<java.util.List<com.project.nagarSetu.util.dto.issue.IssueForWorkerDto>> getAssignedIssues(
            @RequestParam java.util.UUID workerId) {
        return ResponseEntity.ok(workerService.getAssignedIssues(workerId));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PutMapping("/issues/{issueId}/start")
    public ResponseEntity<Boolean> startIssue(@PathVariable java.util.UUID issueId,
            @RequestParam java.util.UUID workerId) {
        return ResponseEntity.ok(workerService.startIssue(workerId, issueId));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PutMapping("/issues/{issueId}/resolve")
    public ResponseEntity<Boolean> resolveIssue(@PathVariable java.util.UUID issueId,
            @RequestParam java.util.UUID workerId) {
        return ResponseEntity.ok(workerService.resolveIssue(workerId, issueId));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PutMapping(value = "/stage")
    public ResponseEntity<Stages> updateStage(
            @RequestPart WorkerUpdateIssue dto,
            @RequestPart(value = "file") MultipartFile file) {
        Stages updatedStage = workerService.updateStage(dto, file);
        return ResponseEntity.ok(updatedStage);
    }

}
