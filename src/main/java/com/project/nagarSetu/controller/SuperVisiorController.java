package com.project.nagarSetu.controller;

import com.project.nagarSetu.service.supervisior.SuperVisiorService;
import com.project.nagarSetu.util.dto.authentication.LoginRequestDto;
import com.project.nagarSetu.util.dto.authentication.RegistrationRequestDto;
import com.project.nagarSetu.util.dto.issue.IssueGetByUserDto;
import com.project.nagarSetu.util.dto.issue.IssueGetDto;
import com.project.nagarSetu.util.dto.user.GetWorkerForSupervisorDto;
import com.project.nagarSetu.util.dto.worker.WorkerCreateResponse;
import com.project.nagarSetu.util.dto.worker.WorkerLoginReponseDto;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.project.nagarSetu.entity.Worker;

@RestController
@AllArgsConstructor
@RequestMapping("/api/supervisior")
public class SuperVisiorController {
    private final SuperVisiorService service;

    @PostMapping("/registration")
    public ResponseEntity<WorkerCreateResponse> registration(
            @Validated @RequestBody RegistrationRequestDto requestDto) {
        WorkerCreateResponse response = service.getRegisterSupervisior(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<WorkerLoginReponseDto> login(
            @Validated @RequestBody LoginRequestDto requestDto) {
        return new ResponseEntity<>(service.getLogin(requestDto), HttpStatus.OK);
    }

    @GetMapping("/getCode")
    public ResponseEntity<Void> getCode(@RequestParam String email, @RequestParam String roles) {
        service.getOTP(email, roles);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueGetDto> getIssue(@PathVariable UUID id, @RequestParam String city) {
        IssueGetDto issueGetDto = service.getIssue(id, city);
        return ResponseEntity.ok(issueGetDto);
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<IssueGetByUserDto>> getIssues(
            @RequestParam String location,
            @RequestParam String stage,
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page) {
        Page<IssueGetByUserDto> issues = service.getIssue(location, stage, type, page);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{supervisiorId}/workers")
    public ResponseEntity<List<GetWorkerForSupervisorDto>> getAllWorkersForSupervisior(@PathVariable UUID supervisiorId) {
        List<GetWorkerForSupervisorDto> workers = service.getAllWorkersForSupervisior(supervisiorId);
        return ResponseEntity.ok(workers);
    }

}
