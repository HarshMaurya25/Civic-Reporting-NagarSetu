package com.project.nagarSetu.service.supervisior;

import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.entity.IssueStageHistory;
import com.project.nagarSetu.entity.Supervisior;
import com.project.nagarSetu.entity.User;
import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.error.exception.IssueNotFoundException;
import com.project.nagarSetu.error.exception.VerificationCodeExpiredException;
import com.project.nagarSetu.event.SendPinEvents;
import com.project.nagarSetu.event.SendPinJobEvents;
import com.project.nagarSetu.repository.IssueRepository;
import com.project.nagarSetu.repository.IssueStageHistoryRepository;
import com.project.nagarSetu.repository.SupervisiorRepository;
import com.project.nagarSetu.repository.UserRepository;
import com.project.nagarSetu.repository.WardRepository;
import com.project.nagarSetu.repository.WorkerRepository;
import com.project.nagarSetu.service.authenication.JwtService;
import com.project.nagarSetu.service.authenication.UserDetail;
import com.project.nagarSetu.service.redis.RedisService;
import com.project.nagarSetu.util.dto.authentication.LoginRequestDto;
import com.project.nagarSetu.util.dto.authentication.RegistrationRequestDto;
import com.project.nagarSetu.util.dto.issue.IssueGetByUserDto;
import com.project.nagarSetu.util.dto.issue.IssueGetDto;
import com.project.nagarSetu.util.dto.issue.IssueMatrixBucketDto;
import com.project.nagarSetu.util.dto.issue.IssueMatrixSummaryDto;
import com.project.nagarSetu.util.dto.issue.IssueStageMatrixDto;
import com.project.nagarSetu.util.dto.issue.WardMatrixRowDto;
import com.project.nagarSetu.util.dto.user.GetWorkerForSupervisorDto;
import com.project.nagarSetu.util.dto.worker.WorkerCreateResponse;
import com.project.nagarSetu.util.dto.worker.WorkerLoginReponseDto;
import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class SuperVisiorService {

    private final SupervisiorRepository supervisiorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    private final WorkerRepository workerRepository;
    private final IssueStageHistoryRepository issueStageHistoryRepository;
    private final WardRepository wardRepository;

    private final String userName = "SUPERVISIOR_";

    public void getOTP(String email, String worker) {
        SecureRandom random = new SecureRandom();
        Integer code = 100_000 + random.nextInt(900_000);
        String codeString = String.valueOf(code);

        log.trace("Code is {} with mail {} ", codeString, email);
        redisService.set(userName + email, codeString, 600);
        eventPublisher.publishEvent(new SendPinJobEvents(email, codeString, worker));
    }

    @Transactional
    public WorkerCreateResponse getRegisterSupervisior(RegistrationRequestDto requestDto) {
        String code = redisService.get(userName + requestDto.getEmail(), String.class);

        if (code == null) {
            log.error("Verification code has expired for {}", requestDto.getEmail());
            throw new VerificationCodeExpiredException(requestDto.getEmail());
        }

        if (!requestDto.getCode().equals(code)) {
            log.error("Incorrect verification code provided for {}", requestDto.getEmail());
            throw new VerificationCodeExpiredException(requestDto.getEmail());
        }

        redisService.delete(userName + requestDto.getEmail());

        User user = User.builder()
                .fullName(requestDto.getFullName())
                .phoneNumber(requestDto.getPhoneNumber())
                .email(requestDto.getEmail())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .roles(requestDto.getRole())
                .gender(requestDto.getGender())
                .age(requestDto.getAge())
                .location(requestDto.getLocation())
                .enable(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("User created successfully with email: {} (Role: {})", requestDto.getEmail(), requestDto.getRole());

        Supervisior supervisior = Supervisior.builder()
                .id(user.getId())
                .user(user)
                .started(false)
                .build();

        supervisiorRepository.save(supervisior);
        log.info("Supervisior created with ID: {}", supervisior.getId());

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRoles().toString());

        return WorkerCreateResponse.builder()
                .id(supervisior.getId())
                .token(token)
                .roles(user.getRoles())
                .started(supervisior.getStarted())
                .build();
    }

    @Transactional
    public WorkerLoginReponseDto getLogin(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

        if (authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException(dto.getEmail());
        }
        UserDetail userDetails = (UserDetail) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRoles().toString());
        Supervisior supervisior = supervisiorRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(dto.getEmail()));

        return WorkerLoginReponseDto
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .token(token)
                .started(supervisior.getStarted())
                .build();
    }

    @Transactional
    public IssueGetDto getIssue(UUID id, String city) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id.toString()));

        Stages previousStage = issue.getStages();
        if (issue.getStages().equals(Stages.PENDING) && issue.getLocation().equals(city)) {
            issue.setStages(Stages.ACKNOWLEDGED);
            if (issue.getAcknowledgedAt() == null) {
                issue.setAcknowledgedAt(LocalDateTime.now());
            }
            if (issue.getFirstResponseAt() == null) {
                issue.setFirstResponseAt(LocalDateTime.now());
            }
            issueStageHistoryRepository.save(IssueStageHistory.builder()
                    .issue(issue)
                    .fromStage(previousStage)
                    .toStage(issue.getStages())
                    .actorType("SUPERVISOR")
                    .actorId(issue.getSupervisior() != null ? issue.getSupervisior().getId() : null)
                    .note("Supervisor acknowledged issue")
                    .build());
        }

        issueRepository.save(issue);

        IssueGetDto getDto = IssueGetDto
                .builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .issueType(issue.getIssueType())
                .description(issue.getDescription())
                .criticality(issue.getCriticality())
                .location(issue.getLocation())
                .latitude(issue.getLatitude())
                .longitude(issue.getLongitude())
                .stages(issue.getStages())
                .submittedBy(issue.getSubmittedBy().getFullName())
                .createAt(issue.getCreateAt())
                .imageUrl(issue.getSecureURL())
                .build();

        return getDto;
    }

    @Transactional
    public Page<IssueGetByUserDto> getIssue(String location, String stage, String type, int page) {
        if (page < 0) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, 15);
        Page<IssueGetByUserDto> issue = issueRepository.getIssueByLocationStageAndType(location, stage, type, pageable);
        return issue;
    }

    public List<GetWorkerForSupervisorDto> getAllWorkersForSupervisior(UUID supervisiorId) {
        return workerRepository.findTheWorker(supervisiorId);
    }

    @Transactional
    public IssueMatrixSummaryDto getIssueMatrixSummary(UUID supervisorId, UUID wardId) {
        UUID effectiveWardId = wardId;
        if (effectiveWardId == null && supervisorId != null) {
            effectiveWardId = wardRepository.findBySupervisor_Id(supervisorId)
                    .map(com.project.nagarSetu.entity.Ward::getId)
                    .orElse(null);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String wardFilter = effectiveWardId == null ? null : effectiveWardId.toString();

        return IssueMatrixSummaryDto.builder()
                .wardId(effectiveWardId)
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

    @Transactional
    public List<WardMatrixRowDto> getWardWiseMatrix(UUID supervisorId, int days) {
        int safeDays = days <= 0 ? 30 : Math.min(days, 365);
        LocalDateTime until = LocalDateTime.now();
        LocalDateTime since = until.minusDays(safeDays);

        Optional<com.project.nagarSetu.entity.Ward> wardOpt = wardRepository.findBySupervisor_Id(supervisorId);
        if (wardOpt.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        com.project.nagarSetu.entity.Ward ward = wardOpt.get();
        String wardFilter = ward.getId().toString();

        long reported = safeCount(issueRepository.countReportedIssues(wardFilter, since, until));
        long solved = safeCount(issueRepository.countSolvedIssues(wardFilter, since, until));
        long inBetween = safeCount(issueRepository.countInBetweenIssues(wardFilter, since, until));
        long slaBreached = safeCount(issueRepository.countSlaBreached(wardFilter, since, until));

        WardMatrixRowDto row = WardMatrixRowDto.builder()
                .wardId(ward.getId())
                .wardName(ward.getName())
                .reported(reported)
                .solved(solved)
                .inBetween(inBetween)
                .slaBreached(slaBreached)
                .build();

        return java.util.List.of(row);
    }

    @Transactional
    public List<IssueStageMatrixDto> getStageMatrixForSupervisor(UUID supervisorId, int days) {
        int safeDays = days <= 0 ? 7 : Math.min(days, 365);
        LocalDateTime until = LocalDateTime.now();
        LocalDateTime since = until.minusDays(safeDays);

        UUID wardId = wardRepository.findBySupervisor_Id(supervisorId)
                .map(com.project.nagarSetu.entity.Ward::getId)
                .orElse(null);
        String wardFilter = wardId == null ? null : wardId.toString();
        return issueRepository.getStageCountsForPeriod(wardFilter, since, until);
    }

    @Transactional
    public long getSlaBreachedForSupervisor(UUID supervisorId, int days) {
        int safeDays = days <= 0 ? 30 : Math.min(days, 365);
        LocalDateTime until = LocalDateTime.now();
        LocalDateTime since = until.minusDays(safeDays);

        UUID wardId = wardRepository.findBySupervisor_Id(supervisorId)
                .map(com.project.nagarSetu.entity.Ward::getId)
                .orElse(null);
        String wardFilter = wardId == null ? null : wardId.toString();
        return safeCount(issueRepository.countSlaBreached(wardFilter, since, until));
    }

}
