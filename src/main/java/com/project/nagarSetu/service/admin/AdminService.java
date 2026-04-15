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
import com.project.nagarSetu.util.dto.issue.IssueMatrixBucketDto;
import com.project.nagarSetu.util.dto.issue.IssueMatrixSummaryDto;
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
import com.project.nagarSetu.util.dto.admin.AdminStatsOverviewDto;
import com.project.nagarSetu.util.dto.admin.UserBasicDetailDto;
import com.project.nagarSetu.util.dto.admin.WardDetailDto;
import com.project.nagarSetu.util.enums.Roles;

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
        private final com.project.nagarSetu.repository.WardRepository wardRepository;

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
                return supervisiorRepository.findAllSupervisors();
        }

        public List<com.project.nagarSetu.util.dto.admin.AdminWorkerDto> getAllSupervisorsWithNoStart() {
                return supervisiorRepository.findAllSupervisorsNoStart();
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

        @org.springframework.transaction.annotation.Transactional
        public Boolean allocateWardToSupervisor(UUID wardId, UUID supervisorId) {
                com.project.nagarSetu.entity.Ward ward = wardRepository.findById(wardId)
                                .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + wardId));
                Supervisior supervisior = supervisiorRepository.findById(supervisorId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Supervisor not found with id: " + supervisorId));

                // If supervisor is already linked to another ward, unlink it first
                wardRepository.findBySupervisor_Id(supervisorId).ifPresent(oldWard -> {
                        if (!oldWard.getId().equals(wardId)) {
                                oldWard.setSupervisor(null);
                                wardRepository.saveAndFlush(oldWard);
                        }
                });

                ward.setSupervisor(supervisior);
                wardRepository.save(ward);
                return true;
        }

        @org.springframework.transaction.annotation.Transactional
        public Boolean updateSupervisor(UUID supervisorId,
                        com.project.nagarSetu.util.dto.admin.UpdateSupervisorDto dto) {
                Supervisior supervisior = supervisiorRepository.findById(supervisorId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Supervisor not found with id: " + supervisorId));

                if (dto.getFullName() != null)
                        supervisior.getUser().setFullName(dto.getFullName());
                if (dto.getPhoneNumber() != null)
                        supervisior.getUser().setPhoneNumber(dto.getPhoneNumber());
                if (dto.getAge() != null)
                        supervisior.getUser().setAge(dto.getAge());
                if (dto.getGender() != null)
                        supervisior.getUser().setGender(dto.getGender());
                if (dto.getLocation() != null)
                        supervisior.getUser().setLocation(dto.getLocation());
                if (dto.getJurisdictionName() != null)
                        supervisior.setJurisdictionName(dto.getJurisdictionName());

                userRepository.save(supervisior.getUser());
                supervisiorRepository.save(supervisior);
                return true;
        }

        @org.springframework.transaction.annotation.Transactional
        public Boolean deleteSupervisor(UUID supervisorId) {
                Supervisior supervisior = supervisiorRepository.findById(supervisorId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Supervisor not found with id: " + supervisorId));

                // 1. Unlink from Ward
                wardRepository.findBySupervisor_Id(supervisorId).ifPresent(ward -> {
                        ward.setSupervisor(null);
                        wardRepository.save(ward);
                });

                // 2. Unassign workers
                java.util.List<Worker> workers = workerRepository.findBySupervisior_Id(supervisorId);
                for (Worker w : workers) {
                        w.setSupervisior(null);
                }
                workerRepository.saveAll(workers);

                // 3. Re-assign open issues back to admin fallback
                java.util.List<Issue> pendingIssues = issueRepository.findPendingIssuesBySupervisorId(supervisorId);
                for (Issue issue : pendingIssues) {
                        issue.setSupervisior(null);
                        issue.setAdmin(true);

                        // Also clear assigned workers to restart the matching process fully
                        if (issue.getAssigned() != null) {
                                for (Worker w : issue.getAssigned()) {
                                        w.getIssues().remove(issue);
                                        workerRepository.save(w);
                                }
                                issue.getAssigned().clear();
                        }

                        // We reset stage so that when a new supervisor accepts, it will trigger
                        // auto-assign again
                        com.project.nagarSetu.util.enums.Stages previousStage = issue.getStages();
                        issue.setStages(com.project.nagarSetu.util.enums.Stages.PENDING);

                        // Log history
                        if (previousStage != issue.getStages()) {
                                issueStageHistoryRepository.save(com.project.nagarSetu.entity.IssueStageHistory
                                                .builder()
                                                .issue(issue)
                                                .fromStage(previousStage)
                                                .toStage(issue.getStages())
                                                .actorType("ADMIN")
                                                .note("Returned to fallback queue due to Supervisor deleted")
                                                .build());
                        }

                        issueRepository.save(issue);
                }

                // 4. Disable User and delete Supervisor
                com.project.nagarSetu.entity.User user = supervisior.getUser();
                if (user != null) {
                        user.setEnable(false);
                        userRepository.save(user);
                }
                supervisiorRepository.delete(supervisior);

                return true;
        }

        public List<com.project.nagarSetu.util.dto.admin.AdminWardDto> getAllAdminWards() {
                return wardRepository.findAll().stream().map(ward -> com.project.nagarSetu.util.dto.admin.AdminWardDto
                                .builder()
                                .wardId(ward.getId())
                                .wardName(ward.getName())
                                .region(ward.getRegion())
                                .supervisorId(ward.getSupervisor() != null ? ward.getSupervisor().getId() : null)
                                .supervisorName(ward.getSupervisor() != null && ward.getSupervisor().getUser() != null
                                                ? ward.getSupervisor().getUser().getFullName()
                                                : null)
                                .build()).collect(Collectors.toList());
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

        public WardDetailDto getWardDetail(UUID wardId, String wardName) {
                if ((wardId == null && (wardName == null || wardName.isBlank()))
                                || (wardId != null && wardName != null && !wardName.isBlank())) {
                        throw new IllegalArgumentException("Provide either wardId or wardName");
                }

                com.project.nagarSetu.entity.Ward ward = wardId != null
                                ? wardRepository.findById(wardId)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Ward not found with id: " + wardId))
                                : wardRepository.findByNameIgnoreCase(wardName)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Ward not found with name: " + wardName));

                Supervisior supervisor = ward.getSupervisor();
                String supervisorName = null;
                String supervisorEmail = null;
                String supervisorPhone = null;
                Long workerCount = 0L;

                if (supervisor != null) {
                        if (supervisor.getUser() != null) {
                                supervisorName = supervisor.getUser().getFullName();
                                supervisorEmail = supervisor.getUser().getEmail();
                                supervisorPhone = supervisor.getUser().getPhoneNumber();
                        }
                        workerCount = workerRepository.countBySupervisior_Id(supervisor.getId());
                }

                Long unfinishedIssueCount = issueRepository.countUnresolvedByWardId(ward.getId().toString());
                if (unfinishedIssueCount == null) {
                        unfinishedIssueCount = 0L;
                }

                return WardDetailDto.builder()
                                .wardId(ward.getId())
                                .wardNumber(ward.getName())
                                .supervisorName(supervisorName)
                                .supervisorEmail(supervisorEmail)
                                .supervisorPhoneNumber(supervisorPhone)
                                .workerCount(workerCount)
                                .unfinishedIssueCount(unfinishedIssueCount)
                                .regionName(ward.getRegion())
                                .build();
        }

        public UserBasicDetailDto getSupervisorDetail(UUID supervisorId) {
                Supervisior supervisor = supervisiorRepository.findById(supervisorId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Supervisor not found with id: " + supervisorId));
                return mapUserBasicDetail(supervisor.getUser());
        }

        public UserBasicDetailDto getWorkerDetail(UUID workerId) {
                Worker worker = workerRepository.findById(workerId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Worker not found with id: " + workerId));
                return mapUserBasicDetail(worker.getUser());
        }

        public UserBasicDetailDto getCitizenDetail(UUID userId) {
                com.project.nagarSetu.entity.User user = userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "User not found with id: " + userId));
                if (user.getRoles() != Roles.CITIZEN) {
                        throw new IllegalArgumentException("User is not a citizen");
                }
                return mapUserBasicDetail(user);
        }

        public AdminStatsOverviewDto getAdminOverviewStats() {
                java.time.LocalDateTime startOfMonth = java.time.LocalDate.now()
                                .withDayOfMonth(1)
                                .atStartOfDay();
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                long monthlyReported = safeCount(issueRepository.countReportedIssues(null, startOfMonth, now));
                long monthlySolved = safeCount(issueRepository.countSolvedIssues(null, startOfMonth, now));
                long totalResolved = safeCount(issueRepository.countResolvedIssues());

                return AdminStatsOverviewDto.builder()
                                .wardCount(wardRepository.count())
                                .monthlyReportedIssues(monthlyReported)
                                .monthlySolvedIssues(monthlySolved)
                                .workerCount(workerRepository.count())
                                .supervisorCount(supervisiorRepository.count())
                                .totalIssuesReported(issueRepository.count())
                                .totalIssuesResolved(totalResolved)
                                .build();
        }

        public IssueMatrixSummaryDto getIssueMatrixSummary(UUID wardId) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                String wardFilter = wardId == null ? null : wardId.toString();

                return IssueMatrixSummaryDto.builder()
                                .wardId(wardId)
                                .daily(buildIssueMatrixBucket("daily", wardFilter, now.minusDays(1), now))
                                .weekly(buildIssueMatrixBucket("weekly", wardFilter, now.minusWeeks(1), now))
                                .monthly(buildIssueMatrixBucket("monthly", wardFilter, now.minusMonths(1), now))
                                .build();
        }

        private IssueMatrixBucketDto buildIssueMatrixBucket(String period, String wardFilter,
                        java.time.LocalDateTime since, java.time.LocalDateTime until) {
                long reported = safeCount(issueRepository.countReportedIssues(wardFilter, since, until));
                long solved = safeCount(issueRepository.countSolvedIssues(wardFilter, since, until));
                long inBetween = safeCount(issueRepository.countInBetweenIssues(wardFilter, since, until));

                return IssueMatrixBucketDto.builder()
                                .period(period)
                                .from(since)
                                .to(until)
                                .reported(reported)
                                .solved(solved)
                                .inBetween(inBetween)
                                .build();
        }

        private long safeCount(Long count) {
                return count == null ? 0L : count;
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

        private UserBasicDetailDto mapUserBasicDetail(com.project.nagarSetu.entity.User user) {
                if (user == null) {
                        throw new EntityNotFoundException("User not found");
                }
                return UserBasicDetailDto.builder()
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .phoneNumber(user.getPhoneNumber())
                                .age(user.getAge())
                                .build();
        }
}
