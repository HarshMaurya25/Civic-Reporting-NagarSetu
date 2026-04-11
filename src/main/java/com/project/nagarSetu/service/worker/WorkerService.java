package com.project.nagarSetu.service.worker;

import com.project.nagarSetu.entity.User;
import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.error.exception.IssueNotFoundException;
import com.project.nagarSetu.error.exception.VerificationCodeExpiredException;
import com.project.nagarSetu.event.SendPinEvents;
import com.project.nagarSetu.event.SendPinJobEvents;
import com.project.nagarSetu.repository.UserRepository;
import com.project.nagarSetu.repository.WorkerRepository;
import com.project.nagarSetu.repository.IssueStageHistoryRepository;
import com.project.nagarSetu.service.authenication.JwtService;
import com.project.nagarSetu.service.authenication.UserDetail;
import com.project.nagarSetu.service.image.ImageService;
import com.project.nagarSetu.service.redis.RedisService;
import com.project.nagarSetu.util.dto.authentication.LoginRequestDto;
import com.project.nagarSetu.util.dto.authentication.LoginResponseDto;
import com.project.nagarSetu.util.dto.authentication.RegisterResponse;
import com.project.nagarSetu.util.dto.authentication.RegistrationRequestDto;
import com.project.nagarSetu.util.dto.worker.WorkerCreateResponse;
import com.project.nagarSetu.util.dto.worker.WorkerLoginReponseDto;
import com.project.nagarSetu.util.dto.worker.WorkerUpdateIssue;
import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.entity.IssueStageHistory;
import com.project.nagarSetu.util.dto.issue.IssueForWorkerDto;
import com.project.nagarSetu.repository.IssueRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@AllArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final ImageService imageService;
    private final IssueStageHistoryRepository issueStageHistoryRepository;

    private final String userName = "WORKER_";

    public void getOTP(String email, String role) {
        SecureRandom random = new SecureRandom();
        Integer code = 100_000 + random.nextInt(900_000);
        String codeString = String.valueOf(code);

        log.trace("Code is {} with mail {} ", codeString, email);
        redisService.set(userName + email, codeString, 600);
        eventPublisher.publishEvent(new SendPinJobEvents(email, codeString, role));
    }

    public WorkerCreateResponse getRegisterWorker(RegistrationRequestDto requestDto) {
        String code = redisService.get(userName + requestDto.getEmail(), String.class);
        if (code == null) {
            log.info("Code is expired");
            throw new VerificationCodeExpiredException(requestDto.getEmail());
        }

        if (!requestDto.getCode().equals(code)) {
            log.info("Wrong verification code provided for {}", requestDto.getEmail());
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
        log.info("User is created with email : {}({})", requestDto.getEmail(), requestDto.getRole());

        Worker worker = Worker
                .builder()
                .id(user.getId())
                .user(user)
                .started(false)
                .build();

        workerRepository.save(worker);

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRoles().toString());

        return WorkerCreateResponse
                .builder()
                .id(worker.getId())
                .token(token)
                .roles(user.getRoles())
                .started(worker.getStarted())
                .build();
    }

    public WorkerLoginReponseDto getLogin(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

        if (authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException(dto.getEmail());
        }
        UserDetail userDetails = (UserDetail) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRoles().toString());
        Worker worker = workerRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(dto.getEmail()));

        return WorkerLoginReponseDto
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .token(token)
                .started(worker.getStarted())
                .build();
    }

    public java.util.List<IssueForWorkerDto> getAssignedIssues(java.util.UUID workerId) {
        if (workerId == null)
            throw new IllegalArgumentException("workerId is required");
        workerRepository.findById(workerId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Worker not found"));
        return issueRepository.getAssignedOpenIssues(workerId);
    }

    public Boolean startIssue(java.util.UUID workerId, java.util.UUID issueId) {
        if (workerId == null || issueId == null)
            throw new IllegalArgumentException("ids required");
        com.project.nagarSetu.entity.Worker w = workerRepository.findById(workerId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Worker not found"));
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Issue not found"));

        if (issue.getAssigned() == null || !issue.getAssigned().stream().anyMatch(x -> x.getId().equals(workerId))) {
            throw new IllegalStateException("Worker is not assigned to the issue");
        }

        Stages previousStage = issue.getStages();
        issue.setStages(com.project.nagarSetu.util.enums.Stages.IN_PROGRESS);
        if (issue.getInProgressAt() == null) {
            issue.setInProgressAt(LocalDateTime.now());
        }
        if (issue.getFirstResponseAt() == null) {
            issue.setFirstResponseAt(LocalDateTime.now());
        }
        recordStageTransition(issue, previousStage, issue.getStages(), "WORKER", workerId, "Worker started issue");
        issueRepository.save(issue);

        // notify supervisor
        if (issue.getSupervisior() != null && issue.getSupervisior().getUser() != null
                && issue.getSupervisior().getUser().getEmail() != null) {
            eventPublisher.publishEvent(new com.project.nagarSetu.event.IssueAssignedDataEvent(
                    issue.getSupervisior().getUser().getEmail(),
                    "SUPERVISOR",
                    issue.getId(),
                    issue.getTitle(),
                    "Worker " + w.getUser().getFullName() + " started work.",
                    issue.getCriticality().toString(),
                    issue.getSecureURL(),
                    w.getUser().getFullName(),
                    w.getId(),
                    issue.getSupervisior().getUser().getFullName()));
        }

        return true;
    }

    public Boolean resolveIssue(java.util.UUID workerId, java.util.UUID issueId) {
        if (workerId == null || issueId == null)
            throw new IllegalArgumentException("ids required");
        com.project.nagarSetu.entity.Worker w = workerRepository.findById(workerId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Worker not found"));
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Issue not found"));

        if (issue.getAssigned() == null || !issue.getAssigned().stream().anyMatch(x -> x.getId().equals(workerId))) {
            throw new IllegalStateException("Worker is not assigned to the issue");
        }

        Stages previousStage = issue.getStages();
        issue.setStages(com.project.nagarSetu.util.enums.Stages.RESOLVED);
        issue.setResolvedAt(java.time.LocalDateTime.now());
        if (issue.getTargetResolutionMinutes() != null && issue.getCreateAt() != null
                && issue.getResolvedAt() != null) {
            long actualMinutes = java.time.Duration.between(issue.getCreateAt(), issue.getResolvedAt()).toMinutes();
            issue.setSlaBreached(actualMinutes > issue.getTargetResolutionMinutes());
        }
        recordStageTransition(issue, previousStage, issue.getStages(), "WORKER", workerId, "Worker resolved issue");
        issueRepository.save(issue);

        // notify supervisor and worker
        if (issue.getSupervisior() != null && issue.getSupervisior().getUser() != null
                && issue.getSupervisior().getUser().getEmail() != null) {
            eventPublisher.publishEvent(new com.project.nagarSetu.event.IssueAssignedDataEvent(
                    issue.getSupervisior().getUser().getEmail(),
                    "SUPERVISOR",
                    issue.getId(),
                    issue.getTitle(),
                    "Worker " + w.getUser().getFullName() + " marked issue as resolved.",
                    issue.getCriticality().toString(),
                    issue.getSecureURL(),
                    w.getUser().getFullName(),
                    w.getId(),
                    issue.getSupervisior().getUser().getFullName()));
        }

        if (w.getUser() != null && w.getUser().getEmail() != null) {
            eventPublisher.publishEvent(new com.project.nagarSetu.event.IssueAssignedDataEvent(
                    w.getUser().getEmail(),
                    "WORKER",
                    issue.getId(),
                    issue.getTitle(),
                    "You have successfully resolved the issue.",
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

    @Transactional
    public Stages updateStage(WorkerUpdateIssue dto, MultipartFile file) {
        if (!workerRepository.existsById(dto.getWorkerId())) {
            throw new UsernameNotFoundException(dto.getWorkerId().toString());
        }

        Issue issue = issueRepository.findById(dto.getIssueId())
                .orElseThrow(() -> new IssueNotFoundException("null"));

        if (issue.getAssigned() == null
                || issue.getAssigned().stream().noneMatch(x -> x.getId().equals(dto.getWorkerId()))) {
            throw new IllegalStateException("Worker is not assigned to the issue");
        }

        Stages previousStage = issue.getStages();
        if (!isTransitionAllowed(previousStage, dto.getStages())) {
            throw new IllegalStateException("Invalid stage transition: " + previousStage + " -> " + dto.getStages());
        }

        issue.setStages(dto.getStages());
        issue.setDescription(dto.getDescription());

        if (dto.getStages() == Stages.IN_PROGRESS) {
            if (issue.getInProgressAt() == null) {
                issue.setInProgressAt(LocalDateTime.now());
            }
            if (issue.getFirstResponseAt() == null) {
                issue.setFirstResponseAt(LocalDateTime.now());
            }
        }

        if (dto.getStages() == Stages.TEAM_ASSIGNED && issue.getAssignedAt() == null) {
            issue.setAssignedAt(LocalDateTime.now());
        }

        if (dto.getStages() == Stages.ACKNOWLEDGED) {
            if (issue.getAcknowledgedAt() == null) {
                issue.setAcknowledgedAt(LocalDateTime.now());
            }
            if (issue.getFirstResponseAt() == null) {
                issue.setFirstResponseAt(LocalDateTime.now());
            }
        }

        Map<String, String> map = imageService.saveImage(file, issue.getId());

        issue.setSecureURL(map.get("secure_url").toString());
        issue.setFormat(map.get("format").toString());

        if (dto.getStages() == Stages.RESOLVED) {
            issue.setResolvedAt(LocalDateTime.now());
            if (issue.getTargetResolutionMinutes() != null && issue.getCreateAt() != null
                    && issue.getResolvedAt() != null) {
                long actualMinutes = java.time.Duration.between(issue.getCreateAt(), issue.getResolvedAt()).toMinutes();
                issue.setSlaBreached(actualMinutes > issue.getTargetResolutionMinutes());
            }

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
                                issue.getSupervisior() != null ? issue.getSupervisior().getUser().getFullName()
                                        : null));
                    }
                }
            }
        }

        recordStageTransition(issue, previousStage, dto.getStages(), "WORKER", dto.getWorkerId(),
                "Worker updated stage");

        issueRepository.save(issue);

        return dto.getStages();
    }

    private boolean isTransitionAllowed(Stages from, Stages to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        Set<Stages> nextAllowed = switch (from) {
            case PENDING -> Set.of(Stages.ACKNOWLEDGED, Stages.TEAM_ASSIGNED);
            case ACKNOWLEDGED -> Set.of(Stages.TEAM_ASSIGNED, Stages.IN_PROGRESS);
            case TEAM_ASSIGNED -> Set.of(Stages.IN_PROGRESS, Stages.RECONSIDERED);
            case IN_PROGRESS -> Set.of(Stages.RESOLVED, Stages.RECONSIDERED);
            case RECONSIDERED -> Set.of(Stages.TEAM_ASSIGNED, Stages.IN_PROGRESS, Stages.RESOLVED);
            case RESOLVED -> Set.of(Stages.RECONSIDERED);
        };
        return nextAllowed.contains(to);
    }

    private void recordStageTransition(Issue issue, Stages from, Stages to, String actorType, UUID actorId,
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

}
