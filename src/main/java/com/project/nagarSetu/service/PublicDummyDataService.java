package com.project.nagarSetu.service;

import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.entity.Supervisior;
import com.project.nagarSetu.entity.Ward;
import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.repository.IssueRepository;
import com.project.nagarSetu.repository.SupervisiorRepository;
import com.project.nagarSetu.repository.WardRepository;
import com.project.nagarSetu.repository.WorkerRepository;
import com.project.nagarSetu.util.dto.publicdata.IssueDatesDto;
import com.project.nagarSetu.util.dto.publicdata.NameDateDto;
import com.project.nagarSetu.util.dto.publicdata.UpdateDateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicDummyDataService {
    private final WorkerRepository workerRepository;
    private final SupervisiorRepository supervisiorRepository;
    private final WardRepository wardRepository;
    private final IssueRepository issueRepository;

    @Transactional(readOnly = true)
    public List<NameDateDto> listWorkers() {
        return workerRepository.findAll().stream()
                .map(w -> NameDateDto.builder()
                        .id(w.getId())
                        .name(w.getUser() != null ? w.getUser().getFullName() : null)
                        .date(w.getUser() != null ? w.getUser().getCreatedAt() : null)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NameDateDto> listSupervisors() {
        return supervisiorRepository.findAll().stream()
                .map(s -> NameDateDto.builder()
                        .id(s.getId())
                        .name(s.getUser() != null ? s.getUser().getFullName() : null)
                        .date(s.getUser() != null ? s.getUser().getCreatedAt() : null)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NameDateDto> listWards() {
        return wardRepository.findAll().stream()
                .map(w -> NameDateDto.builder()
                        .id(w.getId())
                        .name(w.getName())
                        .date(w.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IssueDatesDto> listIssues() {
        return issueRepository.findAll().stream()
                .map(i -> IssueDatesDto.builder()
                        .id(i.getId())
                        .title(i.getTitle())
                        .stage(i.getStages())
                        .createAt(i.getCreateAt())
                        .acknowledgedAt(i.getAcknowledgedAt())
                        .assignedAt(i.getAssignedAt())
                        .inProgressAt(i.getInProgressAt())
                        .firstResponseAt(i.getFirstResponseAt())
                        .resolvedAt(i.getResolvedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void updateDate(UpdateDateRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (req.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (req.getValue() == null) {
            throw new IllegalArgumentException("value is required");
        }

        String entity = req.getEntity() == null ? "" : req.getEntity().trim().toUpperCase(Locale.ROOT);
        String field = req.getField() == null ? "" : req.getField().trim();

        switch (entity) {
            case "WORKER" -> updateWorkerDate(req.getId(), field, req.getValue());
            case "SUPERVISOR" -> updateSupervisorDate(req.getId(), field, req.getValue());
            case "WARD" -> updateWardDate(req.getId(), field, req.getValue());
            case "ISSUE" -> updateIssueDate(req.getId(), field, req.getValue());
            default -> throw new IllegalArgumentException("Unsupported entity: " + req.getEntity());
        }
    }

    @Transactional
    public void updateDates(List<UpdateDateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("requests is required");
        }
        for (UpdateDateRequest req : requests) {
            updateDate(req);
        }
    }

    private void updateWorkerDate(UUID workerId, String field, java.time.LocalDateTime value) {
        if (!"createdAt".equals(field)) {
            throw new IllegalArgumentException("WORKER supports only field=createdAt");
        }
        Worker w = workerRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Worker not found"));
        if (w.getUser() == null) {
            throw new IllegalStateException("Worker has no linked user");
        }
        w.getUser().setCreatedAt(value);
        workerRepository.save(w);
    }

    private void updateSupervisorDate(UUID supervisorId, String field, java.time.LocalDateTime value) {
        if (!"createdAt".equals(field)) {
            throw new IllegalArgumentException("SUPERVISOR supports only field=createdAt");
        }
        Supervisior s = supervisiorRepository.findById(supervisorId)
                .orElseThrow(() -> new EntityNotFoundException("Supervisor not found"));
        if (s.getUser() == null) {
            throw new IllegalStateException("Supervisor has no linked user");
        }
        s.getUser().setCreatedAt(value);
        supervisiorRepository.save(s);
    }

    private void updateWardDate(UUID wardId, String field, java.time.LocalDateTime value) {
        if (!"createdAt".equals(field)) {
            throw new IllegalArgumentException("WARD supports only field=createdAt");
        }
        Ward w = wardRepository.findById(wardId)
                .orElseThrow(() -> new EntityNotFoundException("Ward not found"));
        w.setCreatedAt(value);
        wardRepository.save(w);
    }

    private void updateIssueDate(UUID issueId, String field, java.time.LocalDateTime value) {
        Issue i = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));
        switch (field) {
            case "createAt" -> i.setCreateAt(value);
            case "acknowledgedAt" -> i.setAcknowledgedAt(value);
            case "assignedAt" -> i.setAssignedAt(value);
            case "inProgressAt" -> i.setInProgressAt(value);
            case "firstResponseAt" -> i.setFirstResponseAt(value);
            case "resolvedAt" -> i.setResolvedAt(value);
            default -> throw new IllegalArgumentException("Unsupported ISSUE field: " + field);
        }
        issueRepository.save(i);
    }
}

