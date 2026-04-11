package com.project.nagarSetu.service.admin;

import com.project.nagarSetu.entity.Supervisior;
import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.entity.IssueAssignmentHistory;
import com.project.nagarSetu.entity.IssueStageHistory;
import com.project.nagarSetu.repository.IssueAssignmentHistoryRepository;
import com.project.nagarSetu.repository.IssueStageHistoryRepository;
import com.project.nagarSetu.repository.SupervisiorRepository;
import com.project.nagarSetu.repository.UserRepository;
import com.project.nagarSetu.repository.WorkerRepository;
import com.project.nagarSetu.service.authenication.JwtService;
import com.project.nagarSetu.service.redis.RedisService;
import com.project.nagarSetu.util.enums.IssueType;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.repository.IssueRepository;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import com.project.nagarSetu.util.dto.admin.AdminUserDto;

@Service
@AllArgsConstructor
public class AdminService {

        private final WorkerRepository workerRepository;
        private final SupervisiorRepository supervisiorRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;
        private final RedisService redisService;
        private final ApplicationEventPublisher eventPublisher;
        private final UserRepository userRepository;
        private final IssueRepository issueRepository;
        private final IssueAssignmentHistoryRepository issueAssignmentHistoryRepository;
        private final IssueStageHistoryRepository issueStageHistoryRepository;

        @org.springframework.transaction.annotation.Transactional
        public boolean acceptWorker(UUID workerId, UUID superVisorId) {
                Worker worker = workerRepository
                                .findById(workerId)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Worker not found with id: " + workerId));

                worker.setStarted(true);

                Supervisior supervisior = supervisiorRepository
                                .findById(superVisorId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Supervisor not found with id: " + superVisorId));

                worker.setSupervisior(supervisior);

                workerRepository.save(worker);

                // Auto-Assign forgotten Admin Fallback Issues
                java.util.List<Issue> fallbackIssues = issueRepository.findBySupervisiorIdAndAdminTrue(superVisorId);
                if (!fallbackIssues.isEmpty()) {
                        if (worker.getIssues() == null) {
                                worker.setIssues(new java.util.HashSet<>());
                        }
                        for (Issue issue : fallbackIssues) {
                                com.project.nagarSetu.util.enums.Stages previousStage = issue.getStages();
                                issue.setAdmin(false);
                                issue.setStages(com.project.nagarSetu.util.enums.Stages.TEAM_ASSIGNED);
                                if (issue.getAssignedAt() == null) {
                                        issue.setAssignedAt(java.time.LocalDateTime.now());
                                }
                                if (issue.getFirstResponseAt() == null) {
                                        issue.setFirstResponseAt(java.time.LocalDateTime.now());
                                }
                                if (issue.getAssigned() == null) {
                                        issue.setAssigned(new java.util.HashSet<>());
                                }
                                issue.getAssigned().add(worker);
                                worker.getIssues().add(issue);
                                issueRepository.save(issue);
                                if (previousStage != issue.getStages()) {
                                        issueStageHistoryRepository.save(IssueStageHistory.builder()
                                                        .issue(issue)
                                                        .fromStage(previousStage)
                                                        .toStage(issue.getStages())
                                                        .actorType("ADMIN")
                                                        .note("Assigned from admin fallback queue")
                                                        .build());
                                }
                                issueAssignmentHistoryRepository.save(IssueAssignmentHistory.builder()
                                                .issue(issue)
                                                .fromWorkerId(null)
                                                .toWorkerId(worker.getId())
                                                .supervisorId(superVisorId)
                                                .action("ADMIN_ASSIGN")
                                                .reason("Assigned from fallback queue")
                                                .build());

                                if (worker.getUser() != null && worker.getUser().getEmail() != null) {
                                        eventPublisher.publishEvent(
                                                        new com.project.nagarSetu.event.IssueAssignedDataEvent(
                                                                        worker.getUser().getEmail(),
                                                                        "WORKER",
                                                                        issue.getId(),
                                                                        issue.getTitle(),
                                                                        issue.getDescription(),
                                                                        issue.getCriticality().toString(),
                                                                        issue.getSecureURL(),
                                                                        null,
                                                                        null,
                                                                        supervisior.getUser() != null
                                                                                        ? supervisior.getUser()
                                                                                                        .getFullName()
                                                                                        : null));
                                }
                        }
                        workerRepository.save(worker);
                }

                return true;
        }

        public Boolean reassignWorker(UUID workerId, UUID superVisorId) {
                Worker worker = workerRepository
                                .findById(workerId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Worker not found with id: " + workerId));

                Supervisior supervisior = supervisiorRepository
                                .findById(superVisorId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Supervisor not found with id: " + superVisorId));

                worker.setSupervisior(supervisior);

                workerRepository.save(worker);

                return true;
        }

        public Boolean acceptSuperVisior(UUID superVisorId,
                        com.project.nagarSetu.util.dto.admin.AcceptSupervisorDto request) {
                Supervisior supervisior = supervisiorRepository
                                .findById(superVisorId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Supervisor not found with id: " + superVisorId));

                supervisior.setStarted(true);
                supervisior.setDepartment(IssueType.valueOf(request.getDepartment()));
                supervisior.setJurisdictionName(request.getJurisdictionName());
                supervisior.setJurisdictionCenterLat(request.getJurisdictionCenterLat());
                supervisior.setJurisdictionCenterLon(request.getJurisdictionCenterLon());
                supervisior.setJurisdictionRadiusKm(request.getJurisdictionRadiusKm());

                supervisiorRepository.save(supervisior);

                return true;
        }

        public List<AdminUserDto> getAllWorkers() {
                return workerRepository.findAllWorker();
        }

        public List<AdminUserDto> getAllWorkersWithNoStarted() {
                return workerRepository.findAllWorkerNoStart();
        }

        public List<com.project.nagarSetu.util.dto.admin.AdminWorkerDto> getAllSupervisors() {
                return supervisiorRepository.findAllWorker();
        }

        public List<com.project.nagarSetu.util.dto.admin.AdminWorkerDto> getAllSupervisorsWithNoStart() {
                return supervisiorRepository.findAllWorkerNoStart();
        }

        public Boolean reassignIssueWorker(UUID issueId, UUID workerId) {
                Issue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issueId));

                Worker newWorker = workerRepository.findById(workerId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Worker not found with id: " + workerId));

                UUID oldWorkerId = null;

                // remove issue from all currently assigned workers
                if (issue.getAssigned() != null) {
                        for (Worker w : issue.getAssigned()) {
                                oldWorkerId = w.getId();
                                if (w.getIssues() != null) {
                                        w.getIssues().remove(issue);
                                        workerRepository.save(w);
                                }
                        }
                        issue.getAssigned().clear();
                }

                // assign new worker
                if (newWorker.getIssues() == null) {
                        newWorker.setIssues(new java.util.HashSet<>());
                }
                newWorker.getIssues().add(issue);
                workerRepository.save(newWorker);

                if (issue.getAssigned() == null) {
                        issue.setAssigned(new java.util.HashSet<>());
                }
                issue.getAssigned().add(newWorker);
                issue.setSupervisior(newWorker.getSupervisior());
                if (issue.getAssignedAt() == null) {
                        issue.setAssignedAt(java.time.LocalDateTime.now());
                }
                if (issue.getFirstResponseAt() == null) {
                        issue.setFirstResponseAt(java.time.LocalDateTime.now());
                }
                issueRepository.save(issue);

                issueAssignmentHistoryRepository.save(IssueAssignmentHistory.builder()
                                .issue(issue)
                                .fromWorkerId(oldWorkerId)
                                .toWorkerId(newWorker.getId())
                                .supervisorId(issue.getSupervisior() != null ? issue.getSupervisior().getId() : null)
                                .action("ADMIN_REASSIGN")
                                .reason("Manual reassignment by admin")
                                .build());

                // update supervisor last assigned
                if (issue.getSupervisior() != null) {
                        Supervisior s = issue.getSupervisior();
                        s.setLastAssignedWorkerId(newWorker.getId());
                        supervisiorRepository.save(s);

                        // notify supervisor
                        if (s.getUser() != null && s.getUser().getEmail() != null) {
                                String subject = "Worker reassigned to Issue " + issue.getId();
                                String body = "Worker " + newWorker.getUser().getFullName() + " (" + newWorker.getId()
                                                + ") " +
                                                "has been assigned to issue " + issue.getId() + ".";
                                eventPublisher.publishEvent(new com.project.nagarSetu.event.IssueAssignedDataEvent(
                                                s.getUser().getEmail(),
                                                "SUPERVISOR",
                                                issue.getId(),
                                                issue.getTitle(),
                                                issue.getDescription(),
                                                issue.getCriticality().toString(),
                                                issue.getSecureURL(),
                                                newWorker.getUser().getFullName(),
                                                newWorker.getId(),
                                                s.getUser().getFullName()));
                        }
                }

                // notify worker
                if (newWorker.getUser() != null && newWorker.getUser().getEmail() != null) {
                        String subject = "You have been assigned a new issue";
                        String body = "You have been assigned to issue " + issue.getId()
                                        + ". Please check your dashboard.";
                        eventPublisher.publishEvent(new com.project.nagarSetu.event.IssueAssignedDataEvent(
                                        newWorker.getUser().getEmail(),
                                        "WORKER",
                                        issue.getId(),
                                        issue.getTitle(),
                                        issue.getDescription(),
                                        issue.getCriticality().toString(),
                                        issue.getSecureURL(),
                                        null,
                                        null,
                                        issue.getSupervisior() != null && issue.getSupervisior().getUser() != null
                                                        ? issue.getSupervisior().getUser().getFullName()
                                                        : null));
                }

                return true;
        }

        public com.project.nagarSetu.util.dto.issue.Issue30DayStatsDto getIssue30DayStats() {
                java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(30);

                java.util.List<Object[]> createdRaw = issueRepository.getCreatedCountSinceGroupedByDate(since);
                java.util.List<Object[]> resolvedRaw = issueRepository.getResolvedCountSinceGroupedByDate(since);

                java.util.List<com.project.nagarSetu.util.dto.issue.IssueDailyCountDto> created = new java.util.ArrayList<>();
                java.util.List<com.project.nagarSetu.util.dto.issue.IssueDailyCountDto> resolved = new java.util.ArrayList<>();

                for (Object[] row : createdRaw) {
                        created.add(mapToIssueDailyCountDto(row));
                }
                for (Object[] row : resolvedRaw) {
                        resolved.add(mapToIssueDailyCountDto(row));
                }

                return new com.project.nagarSetu.util.dto.issue.Issue30DayStatsDto(created, resolved);
        }

        private com.project.nagarSetu.util.dto.issue.IssueDailyCountDto mapToIssueDailyCountDto(Object[] row) {
                if (row == null || row.length < 2)
                        return new com.project.nagarSetu.util.dto.issue.IssueDailyCountDto((java.time.LocalDate) null,
                                        0L);

                Object dateObj = row[0];
                Object countObj = row[1];
                java.time.LocalDate date = null;

                if (dateObj == null) {
                        date = null;
                } else if (dateObj instanceof java.sql.Date) {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof java.time.LocalDate) {
                        date = (java.time.LocalDate) dateObj;
                } else if (dateObj instanceof java.time.LocalDateTime) {
                        date = ((java.time.LocalDateTime) dateObj).toLocalDate();
                } else if (dateObj instanceof java.sql.Timestamp) {
                        date = ((java.sql.Timestamp) dateObj).toLocalDateTime().toLocalDate();
                } else if (dateObj instanceof java.util.Date) {
                        date = ((java.util.Date) dateObj).toInstant().atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDate();
                } else {
                        // try string parse
                        date = java.time.LocalDate.parse(dateObj.toString());
                }

                Long count = 0L;
                if (countObj instanceof Number) {
                        count = ((Number) countObj).longValue();
                } else {
                        try {
                                count = Long.parseLong(countObj.toString());
                        } catch (Exception e) {
                                count = 0L;
                        }
                }

                return new com.project.nagarSetu.util.dto.issue.IssueDailyCountDto(date, count);
        }
}
