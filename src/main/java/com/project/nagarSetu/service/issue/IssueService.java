package com.project.nagarSetu.service.issue;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.entity.IssueAssignmentHistory;
import com.project.nagarSetu.entity.IssueStageHistory;
import com.project.nagarSetu.entity.User;
import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.error.exception.IssueNotFoundException;
import com.project.nagarSetu.repository.IssueAssignmentHistoryRepository;
import com.project.nagarSetu.repository.IssueRepository;
import com.project.nagarSetu.repository.IssueStageHistoryRepository;
import com.project.nagarSetu.repository.UserRepository;
import com.project.nagarSetu.repository.SupervisiorRepository;
import com.project.nagarSetu.entity.Supervisior;
import com.project.nagarSetu.service.image.ImageService;
import com.project.nagarSetu.service.redis.RedisService;
import org.springframework.context.ApplicationEventPublisher;
import com.project.nagarSetu.util.dto.issue.*;
import com.project.nagarSetu.util.dto.user.UserLeaderboardDto;
import com.project.nagarSetu.util.dto.user.UserMatrixDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final IssueRepository issueRepository;
    private final SupervisiorRepository supervisiorRepository;
    private final com.project.nagarSetu.repository.WorkerRepository workerRepository;
    private final Cloudinary cloudinary;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;
    private final IssueStageHistoryRepository issueStageHistoryRepository;
    private final IssueAssignmentHistoryRepository issueAssignmentHistoryRepository;
    private final com.project.nagarSetu.service.WardService wardService;

    private final String issueImg = "ISSUE_IMG_";

    @Value("${assignment.scoring.load-weight-per-active-issue:100.0}")
    private double loadWeightPerActiveIssue;

    @Value("${assignment.scoring.high-critical-distance-weight:50.0}")
    private double highCriticalDistanceWeight;

    @Value("${assignment.scoring.normal-distance-weight:10.0}")
    private double normalDistanceWeight;

    @Value("${assignment.scoring.fairness-hourly-bonus:2.0}")
    private double fairnessHourlyBonus;

    @Value("${assignment.scoring.no-assignment-fairness-bonus:500.0}")
    private double noAssignmentFairnessBonus;

    @Value("${assignment.scoring.starvation-gap-threshold:10}")
    private int starvationGapThreshold;

    private static final long MAX_MEDIA_SIZE = 5L * 1024 * 1024;
    private static final int PAGE_SIZE = 15;
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private void validateContentMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Content media is required");
        }

        if (file.getSize() > MAX_MEDIA_SIZE) {
            throw new IllegalArgumentException("Content media must be less than 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MEDIA_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG and WEBP are allowed");
        }
    }

    @Transactional
    public UUID createIssue(IssueCreateDto dto, List<MultipartFile> files) {

        User submittedBy = userRepository.findByUserId(dto.getSubmittedById());
        if (submittedBy == null) {
            throw new IllegalArgumentException("User not found with ID: " + dto.getSubmittedById());
        }

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                validateContentMedia(file);
            }
        }

        Issue issue = Issue.builder()
                .title(dto.getTitle())
                .issueType(dto.getIssueType())
                .description(dto.getDescription())
                .criticality(dto.getCriticality())
                .location(dto.getLocation())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .targetResolutionMinutes(dto.getTargetResolutionMinutes())
                .submittedBy(submittedBy)
                .build();

        Optional<com.project.nagarSetu.entity.Ward> optWard = wardService.getWardByLocation(
                dto.getLatitude(), dto.getLongitude());
        if (optWard.isPresent()) {
            issue.setWardId(optWard.get().getId().toString());
        }

        issueRepository.save(issue);

        if (files != null && !files.isEmpty()) {
            for (int i = 0; i < files.size() && i < 3; i++) {
                MultipartFile file = files.get(i);
                Map<String, String> map = imageService.saveImage(file, UUID.randomUUID());
                if (i == 0) {
                    issue.setSecureURL(map.get("secure_url").toString());
                    issue.setFormat(map.get("format").toString());
                } else {
                    issue.getAdditionalUrls().add(map.get("secure_url").toString());
                }
            }
        }

        assignWorkerSmartLogic(issue);

        return issue.getId();
    }

    @Transactional
    public UUID doneIssue(UUID id, MultipartFile file) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Issue not found"));

        validateContentMedia(file);

        com.project.nagarSetu.util.enums.Stages previousStage = issue.getStages();
        issue.setStages(com.project.nagarSetu.util.enums.Stages.RESOLVED);
        stampStageTimestamp(issue, com.project.nagarSetu.util.enums.Stages.RESOLVED);
        recordStageTransition(issue, previousStage, com.project.nagarSetu.util.enums.Stages.RESOLVED, "SYSTEM", null,
                "Issue marked done via doneIssue endpoint");

        Map params = ObjectUtils.asMap(
                "public_id", issueImg + id + "_resolved",
                "overwrite", true,
                "resource_type", "image");

        Map<String, String> map = imageService.saveImage(file, UUID.randomUUID());
        if (map != null && map.containsKey("secure_url") && map.get("secure_url") != null) {
            issue.setResolvedSecureURL(map.get("secure_url").toString());
            if (map.containsKey("format") && map.get("format") != null) {
                issue.setResolvedFormat(map.get("format").toString());
            }
        }
        issueRepository.save(issue);

        if (issue.getSupervisior() != null && issue.getSupervisior().getUser() != null) {
            String supEmail = issue.getSupervisior().getUser().getEmail();
            if (supEmail != null) {
                eventPublisher.publishEvent(new com.project.nagarSetu.event.IssueAssignedDataEvent(
                        supEmail,
                        "SUPERVISOR_DONE",
                        issue.getId(),
                        issue.getTitle(),
                        issue.getDescription(),
                        issue.getCriticality().toString(),
                        issue.getSecureURL(),
                        null,
                        null,
                        issue.getSupervisior().getUser().getFullName()));
            }
        }

        if (issue.getAssigned() != null) {
            for (com.project.nagarSetu.entity.Worker worker : issue.getAssigned()) {
                if (worker.getUser() != null && worker.getUser().getEmail() != null) {
                    eventPublisher.publishEvent(new com.project.nagarSetu.event.IssueAssignedDataEvent(
                            worker.getUser().getEmail(),
                            "WORKER_DONE",
                            issue.getId(),
                            issue.getTitle(),
                            issue.getDescription(),
                            issue.getCriticality().toString(),
                            issue.getSecureURL(),
                            worker.getUser().getFullName(),
                            worker.getId(),
                            issue.getSupervisior() != null ? issue.getSupervisior().getUser().getFullName() : null));
                }
            }
        }

        return id;
    }

    @Transactional
    public Page<IssueSolvedDto> getSolvedIssuesWithImage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : PAGE_SIZE;
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return issueRepository.getSolvedIssuesWithImage(pageable);
    }

    @Transactional
    public IssueGetDto getIssue(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException(id.toString());
        }

        IssueGetDto issue = issueRepository.getIssueById(id);
        if (issue == null) {
            throw new IssueNotFoundException(id.toString());
        }
        return issue;
    }

    @Transactional
    public Page<IssueGetByUserDto> getIssueByUserId(UUID id, int pageNumber) {
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        if (id == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
        Page<IssueGetByUserDto> getIssue = issueRepository.getIssueByUser(id, pageable);
        return getIssue;
    }

    @Transactional
    public Set<IssueByMap> getIssueMapByUser(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        Set<IssueByMap> getIssueMap = issueRepository.getIssueMapByUserId(id, oneMonthAgo);
        return getIssueMap;
    }

    @Transactional
    public UserMatrixDto getMatrix(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        UserMatrixDto getMatrixDto = issueRepository.getIssueMatrix(id);
        return getMatrixDto;
    }

    @Transactional
    public List<UserLeaderboardDto> getLeaderBoard() {
        Pageable pageable = PageRequest.of(0, 5);
        List<UserLeaderboardDto> getLeaderBoard = issueRepository.getLeaderBoard(pageable);
        return getLeaderBoard;
    }

    @Transactional
    public Page<IssueRecent> getRecentIssue(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<IssueRecent> response = issueRepository.getRecentIssue(pageable);
        return response;
    }

    @Transactional
    public java.util.List<com.project.nagarSetu.util.dto.issue.IssueStageCountDto> getWeeklyStageCounts() {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusWeeks(1);
        return issueRepository.getIssueCountSinceGroupedByStage(since);
    }

    @Transactional
    public java.util.Set<IssueByMap> getIssueMapForSupervisor(java.util.UUID wardId) {
        if (wardId == null)
            throw new IllegalArgumentException("wardId is required");
        return issueRepository.getIssueMapForSupervisor(wardId.toString());
    }

    @Transactional
    public java.util.Set<IssueByMap> getIssueMapForWorker(java.util.UUID workerId) {
        if (workerId == null)
            throw new IllegalArgumentException("workerId is required");
        return issueRepository.getAssignedOpenIssueMap(workerId);
    }

    @Transactional
    public java.util.Set<IssueByMap> getIssueMapForAdmin() {
        return issueRepository.getAllIssueMap();
    }

    @Transactional
    public boolean assignWorkerSmartLogic(Issue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("issue is required");
        }

        // 1. Find Ward and Area Head Supervisor
        Optional<com.project.nagarSetu.entity.Ward> optWard = wardService.getWardByLocation(
                issue.getLatitude(), issue.getLongitude());

        if (optWard.isEmpty()) {
            log.warn("No valid ward found for coordinates: [{}, {}]",
                    issue.getLatitude(), issue.getLongitude());
            issue.setAdmin(true);
            issueRepository.save(issue);
            return true;
        }

        com.project.nagarSetu.entity.Ward ward = optWard.get();
        issue.setWardId(ward.getId().toString());

        Supervisior supervisor = ward.getSupervisor();
        if (supervisor == null) {
            log.warn("Ward {} has no assigned supervisor", ward.getName());
            issue.setAdmin(true);
            issueRepository.save(issue);
            return true;
        }

        issue.setSupervisior(supervisor);

        // 2. Fetch Scorable Workers
        List<com.project.nagarSetu.util.dto.issue.WorkerScoringDto> candidates = workerRepository
                .findScorableWorkers(supervisor.getId());

        if (candidates.isEmpty()) {
            log.warn("No active workers found under Supervisor: {}", supervisor.getId());
            issue.setAdmin(true);
            issueRepository.save(issue);
            return true;
        }

        UUID selectedWorkerId = null;
        double lowestScore = Double.MAX_VALUE;
        LocalDateTime oldestAssignment = LocalDateTime.MAX;

        int hardLimitGap = starvationGapThreshold;
        boolean hardLimitTriggered = false;

        for (com.project.nagarSetu.util.dto.issue.WorkerScoringDto candidate : candidates) {

            // 1. Hard Limit Starvation Rule
            int candidateLifeTimeAsgn = candidate.getLifetimeAssignments() != null ? candidate.getLifetimeAssignments()
                    : 0;
            int superTotalAsgn = supervisor.getTotalAssignmentsDispatched() != null
                    ? supervisor.getTotalAssignmentsDispatched()
                    : 0;
            int candidateGap = superTotalAsgn - candidateLifeTimeAsgn;

            if (candidateGap > hardLimitGap && !hardLimitTriggered) {
                selectedWorkerId = candidate.getWorkerId();
                hardLimitTriggered = true;
                lowestScore = Double.MIN_VALUE;
                oldestAssignment = candidate.getLastAssignedAt() != null ? candidate.getLastAssignedAt()
                        : LocalDateTime.MIN;
                continue;
            }

            if (hardLimitTriggered) {
                if (candidateGap > hardLimitGap) {
                    LocalDateTime candLast = candidate.getLastAssignedAt() != null ? candidate.getLastAssignedAt()
                            : LocalDateTime.MIN;
                    if (candLast.isBefore(oldestAssignment)) {
                        selectedWorkerId = candidate.getWorkerId();
                        oldestAssignment = candLast;
                    }
                }
                continue;
            }

            // 2. Normal Scoring Phase
            double loadScore = loadWeightPerActiveIssue *
                    (candidate.getActiveIssueCount() != null ? candidate.getActiveIssueCount() : 0);

            double distanceScore = 0.0;
            if (candidate.getLatitude() != null && candidate.getLongitude() != null) {
                double distKm = calculateHaversine(issue.getLatitude(), issue.getLongitude(), candidate.getLatitude(),
                        candidate.getLongitude());
                double distWeight = issue.getCriticality() == com.project.nagarSetu.util.enums.Criticality.HIGH
                        ? highCriticalDistanceWeight
                        : normalDistanceWeight;
                distanceScore = distWeight * distKm;
            }

            // Fairness Bonus (aging)
            double fairnessBonus = 0.0;
            if (candidate.getLastAssignedAt() != null) {
                long hoursSince = java.time.Duration.between(candidate.getLastAssignedAt(), LocalDateTime.now())
                        .toHours();
                fairnessBonus = (hoursSince > 0 ? hoursSince : 0) * fairnessHourlyBonus;
            } else {
                fairnessBonus = noAssignmentFairnessBonus;
            }

            // Formula Calculation
            double finalScore = loadScore + distanceScore - fairnessBonus;

            // Tie-breaker and evaluation
            if (finalScore < lowestScore) {
                lowestScore = finalScore;
                selectedWorkerId = candidate.getWorkerId();
                oldestAssignment = candidate.getLastAssignedAt() != null ? candidate.getLastAssignedAt()
                        : LocalDateTime.MIN;
            } else if (finalScore == lowestScore) {
                LocalDateTime candDate = candidate.getLastAssignedAt() != null ? candidate.getLastAssignedAt()
                        : LocalDateTime.MIN;
                if (candDate.isBefore(oldestAssignment)) {
                    selectedWorkerId = candidate.getWorkerId();
                    oldestAssignment = candDate;
                }
            }
        }

        // Persist assignments
        Worker chosenWorker = workerRepository.findById(selectedWorkerId)
                .orElseThrow(() -> new IllegalStateException("Selected worker vanished during assignment phase"));

        if (issue.getAssigned() == null)
            issue.setAssigned(new java.util.HashSet<>());
        issue.getAssigned().add(chosenWorker);
        com.project.nagarSetu.util.enums.Stages previousStage = issue.getStages();
        issue.setStages(com.project.nagarSetu.util.enums.Stages.TEAM_ASSIGNED);
        stampStageTimestamp(issue, com.project.nagarSetu.util.enums.Stages.TEAM_ASSIGNED);
        recordStageTransition(issue, previousStage, com.project.nagarSetu.util.enums.Stages.TEAM_ASSIGNED, "SYSTEM",
                null,
                "Auto-assigned worker using scoring logic");
        recordAssignmentTransition(issue, null, chosenWorker.getId(), supervisor.getId(), "AUTO_ASSIGN", null);

        if (chosenWorker.getIssues() == null)
            chosenWorker.setIssues(new java.util.HashSet<>());
        chosenWorker.getIssues().add(issue);
        chosenWorker.setLastAssignedAt(LocalDateTime.now());
        chosenWorker.setLifetimeAssignments(
                (chosenWorker.getLifetimeAssignments() == null ? 0 : chosenWorker.getLifetimeAssignments()) + 1);
        workerRepository.save(chosenWorker);

        supervisor.setTotalAssignmentsDispatched(
                (supervisor.getTotalAssignmentsDispatched() == null ? 0 : supervisor.getTotalAssignmentsDispatched())
                        + 1);
        supervisiorRepository.save(supervisor);
        issueRepository.save(issue);

        // Notify Supervisor
        if (supervisor.getUser() != null && supervisor.getUser().getEmail() != null) {
            eventPublisher.publishEvent(
                    new com.project.nagarSetu.event.IssueAssignedDataEvent(
                            supervisor.getUser().getEmail(),
                            "SUPERVISOR",
                            issue.getId(),
                            issue.getTitle(),
                            issue.getDescription(),
                            issue.getCriticality().toString(),
                            issue.getSecureURL(),
                            chosenWorker.getUser().getFullName(),
                            chosenWorker.getId(),
                            supervisor.getUser().getFullName()));
        }

        // Notify Worker
        if (chosenWorker.getUser() != null && chosenWorker.getUser().getEmail() != null) {
            eventPublisher.publishEvent(
                    new com.project.nagarSetu.event.IssueAssignedDataEvent(
                            chosenWorker.getUser().getEmail(),
                            "WORKER",
                            issue.getId(),
                            issue.getTitle(),
                            issue.getDescription(),
                            issue.getCriticality().toString(),
                            issue.getSecureURL(),
                            null,
                            null,
                            supervisor.getUser() != null ? supervisor.getUser().getFullName() : null));
        }

        return true;
    }

    private double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void stampStageTimestamp(Issue issue, com.project.nagarSetu.util.enums.Stages stage) {
        LocalDateTime now = LocalDateTime.now();
        switch (stage) {
            case ACKNOWLEDGED -> {
                if (issue.getAcknowledgedAt() == null) {
                    issue.setAcknowledgedAt(now);
                }
                if (issue.getFirstResponseAt() == null) {
                    issue.setFirstResponseAt(now);
                }
            }
            case TEAM_ASSIGNED -> {
                if (issue.getAssignedAt() == null) {
                    issue.setAssignedAt(now);
                }
                if (issue.getFirstResponseAt() == null) {
                    issue.setFirstResponseAt(now);
                }
            }
            case IN_PROGRESS -> {
                if (issue.getInProgressAt() == null) {
                    issue.setInProgressAt(now);
                }
                if (issue.getFirstResponseAt() == null) {
                    issue.setFirstResponseAt(now);
                }
            }
            case RESOLVED -> issue.setResolvedAt(now);
            default -> {
            }
        }
        if (stage == com.project.nagarSetu.util.enums.Stages.RESOLVED
                && issue.getTargetResolutionMinutes() != null
                && issue.getCreateAt() != null
                && issue.getResolvedAt() != null) {
            long actualMinutes = java.time.Duration.between(issue.getCreateAt(), issue.getResolvedAt()).toMinutes();
            issue.setSlaBreached(actualMinutes > issue.getTargetResolutionMinutes());
        }
    }

    private void recordStageTransition(
            Issue issue,
            com.project.nagarSetu.util.enums.Stages from,
            com.project.nagarSetu.util.enums.Stages to,
            String actorType,
            UUID actorId,
            String note) {
        if (from == null || to == null || from == to) {
            return;
        }
        issueStageHistoryRepository.save(IssueStageHistory.builder()
                .issue(issue)
                .fromStage(from)
                .toStage(to)
                .actorType(actorType)
                .actorId(actorId)
                .note(note)
                .build());
    }

    private void recordAssignmentTransition(
            Issue issue,
            UUID fromWorkerId,
            UUID toWorkerId,
            UUID supervisorId,
            String action,
            String reason) {
        issueAssignmentHistoryRepository.save(IssueAssignmentHistory.builder()
                .issue(issue)
                .fromWorkerId(fromWorkerId)
                .toWorkerId(toWorkerId)
                .supervisorId(supervisorId)
                .action(action)
                .reason(reason)
                .build());
    }

    public GetIssueWorkker getIssueWorker(UUID id) {
        Worker worker = workerRepository.findWorkersByIssueId(id);

        if (worker == null) {
            return GetIssueWorkker
                    .builder()
                    .workerId(null)
                    .workerName("NO ONE")
                    .build();
        }

        return GetIssueWorkker
                .builder()
                .workerId(worker.getId())
                .workerName(worker.getUser().getFullName())
                .build();
    }

    @Transactional
    public void reassignIssue(UUID issueId, UUID newWorkerId) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found"));

        Worker newWorker = workerRepository.findById(newWorkerId)
                .orElseThrow(() -> new UsernameNotFoundException("Worker not found"));

        List<Worker> currentWorkers = Collections.singletonList(workerRepository.findWorkersByIssueId(issueId));

        for (Worker worker : currentWorkers) {
            worker.getIssues().remove(issue);
        }

        newWorker.getIssues().add(issue);

        workerRepository.saveAll(currentWorkers);
        workerRepository.save(newWorker);
    }

    @Transactional
    public boolean upvoteIssue(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found"));
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (issue.getSubmittedBy().getId().equals(user.getId())) {
            return false; // Creator cannot implicitly upvote again
        }

        if (issue.getUpvoters().contains(user)) {
            return false; // Already upvoted
        }

        issue.getUpvoters().add(user);
        issue.setUpvoteCount(issue.getUpvoters().size());

        // Dynamic Criticality Adjustment
        int votes = issue.getUpvoteCount();
        if (votes >= 10 && issue.getCriticality() != com.project.nagarSetu.util.enums.Criticality.HIGH) {
            issue.setCriticality(com.project.nagarSetu.util.enums.Criticality.HIGH);
        } else if (votes >= 5 && issue.getCriticality() == com.project.nagarSetu.util.enums.Criticality.LOW) {
            issue.setCriticality(com.project.nagarSetu.util.enums.Criticality.MEDIUM);
        }

        issueRepository.save(issue);
        return true;
    }

    @Transactional
    public List<IssueGetDto> getNearbyUnresolvedIssues(double lat, double lng,
            com.project.nagarSetu.util.enums.IssueType category) {
        double maxRadiusKm = switch (category) {
            case WASTE_MANAGEMENT -> 0.05;
            case POTHOLE -> 0.5;
            case INFRASTRUCTURE -> 0.1;
            default -> 0.2;
        };

        List<Issue> unresolved = issueRepository.findUnresolvedByCategory(category);
        List<IssueGetDto> nearby = new ArrayList<>();

        for (Issue i : unresolved) {
            double distance = calculateHaversine(lat, lng, i.getLatitude(), i.getLongitude());
            if (distance <= maxRadiusKm) {
                nearby.add(IssueGetDto.builder()
                        .id(i.getId())
                        .title(i.getTitle())
                        .issueType(i.getIssueType())
                        .description(i.getDescription())
                        .criticality(i.getCriticality())
                        .location(i.getLocation())
                        .latitude(i.getLatitude())
                        .longitude(i.getLongitude())
                        .stages(i.getStages())
                        .submittedBy(i.getSubmittedBy() != null ? i.getSubmittedBy().getFullName() : "Unknown")
                        .createAt(i.getCreateAt())
                        .imageUrl(i.getSecureURL())
                        .build());
            }
        }
        return nearby;
    }

}
