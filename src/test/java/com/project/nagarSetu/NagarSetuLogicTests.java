package com.project.nagarSetu;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import com.project.nagarSetu.entity.*;
import com.project.nagarSetu.repository.*;
import com.project.nagarSetu.service.admin.AdminService;
import com.project.nagarSetu.service.image.ImageService;
import com.project.nagarSetu.service.issue.IssueService;
import com.project.nagarSetu.util.dto.issue.WorkerScoringDto;
import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.Stages;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NagarSetuLogicTests {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private SupervisiorRepository supervisiorRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ImageService imageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @InjectMocks
    private IssueService issueService;

    @InjectMocks
    private AdminService adminService;

    private UUID supervisorId = UUID.randomUUID();
    private UUID workerId = UUID.randomUUID();
    private UUID issueId = UUID.randomUUID();

    @BeforeEach
    public void setup() {
        // Prepare some basics if needed
    }

    @Test
    void testSupervisorWorkerAutoAssign_whenFallbackIssuesExist() {
        // Arrange
        Worker newWorker = new Worker();
        newWorker.setId(workerId);
        newWorker.setIssues(new HashSet<>());
        User workerUser = new User(); workerUser.setEmail("worker@test.com");
        newWorker.setUser(workerUser);

        Supervisior supervisor = new Supervisior();
        supervisor.setId(supervisorId);
        
        Issue forgotIssue = new Issue();
        forgotIssue.setId(UUID.randomUUID());
        forgotIssue.setAdmin(true);
        forgotIssue.setCriticality(Criticality.HIGH);

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(newWorker));
        when(supervisiorRepository.findById(supervisorId)).thenReturn(Optional.of(supervisor));
        when(issueRepository.findBySupervisiorIdAndAdminTrue(supervisorId)).thenReturn(List.of(forgotIssue));

        // Act
        boolean result = adminService.acceptWorker(workerId, supervisorId);

        // Assert
        assertTrue(result);
        assertTrue(newWorker.getStarted());
        assertFalse(forgotIssue.isAdmin(), "Admin flag should be stripped from issue");
        assertEquals(Stages.TEAM_ASSIGNED, forgotIssue.getStages(), "Issue must route to TEAM_ASSIGNED");
        verify(workerRepository, atLeastOnce()).save(newWorker);
        verify(issueRepository, times(1)).save(forgotIssue);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void testAssignWorkerSmartLogic_withNoWorkers_assignsToAdmin() {
        // Arrange
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setLatitude(40.0);
        issue.setLongitude(40.0);
        issue.setIssueType(com.project.nagarSetu.util.enums.IssueType.ROAD);

        Supervisior supervisor = new Supervisior();
        supervisor.setId(supervisorId);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(supervisiorRepository.findHeadOfArea(anyDouble(), anyDouble(), anyString())).thenReturn(Optional.of(supervisor));
        lenient().when(workerRepository.findScorableWorkers(supervisorId)).thenReturn(Collections.emptyList());

        // Act
        boolean result = issueService.assignWorkerSmartLogic(issue);

        // Assert
        assertTrue(result);
        assertTrue(issue.isAdmin(), "Issue should fallback to ADMIN if no scorable workers exist");
        assertEquals(supervisor, issue.getSupervisior());
        verify(issueRepository).save(issue);
    }

    @Test
    void testAssignWorkerSmartLogic_withScoreCalculations() {
        // Arrange
        Issue issue = new Issue();
        issue.setId(issueId);
        // Central Point
        issue.setLatitude(19.017615);
        issue.setLongitude(72.856164);
        issue.setCriticality(Criticality.HIGH);
        issue.setIssueType(com.project.nagarSetu.util.enums.IssueType.ROAD);

        Supervisior supervisor = new Supervisior();
        supervisor.setId(supervisorId);
        supervisor.setTotalAssignmentsDispatched(0);

        // Construct 2 Scorable workers
        // Worker A: Very close but has high load
        WorkerScoringDto workerA = new WorkerScoringDto(
            UUID.randomUUID(), "Worker A", 19.018, 72.857, LocalDateTime.now().minusHours(1), 0, 5L
        );
        
        // Worker B: A bit further but completely empty queue
        WorkerScoringDto workerB = new WorkerScoringDto(
            workerId, "Worker B", 19.020, 72.860, LocalDateTime.now().minusDays(1), 0, 0L
        );

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(supervisiorRepository.findHeadOfArea(anyDouble(), anyDouble(), anyString())).thenReturn(Optional.of(supervisor));
        when(workerRepository.findScorableWorkers(supervisorId)).thenReturn(Arrays.asList(workerA, workerB));
        
        Worker wEntity = new Worker(); wEntity.setId(workerB.getWorkerId());
        User wUser = new User(); wUser.setEmail("w2@test.com"); wEntity.setUser(wUser);
        when(workerRepository.findById(workerB.getWorkerId())).thenReturn(Optional.of(wEntity));

        // Act
        boolean result = issueService.assignWorkerSmartLogic(issue);

        // Assert
        assertTrue(result);
        assertFalse(issue.isAdmin());
        assertNotNull(issue.getAssigned());
        // Worker B has 0 active issues, heavily winning the load-balance formula despite being slightly further
        assertTrue(issue.getAssigned().contains(wEntity));
        verify(issueRepository).save(issue);
    }

    @Test
    void testDoneIssue_setsResolvedSuccessfullyAndFiresEvents() {
        // Arrange
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setStages(Stages.IN_PROGRESS);
        issue.setCriticality(Criticality.MEDIUM);
        
        Supervisior sup = new Supervisior(); User sUser = new User(); sUser.setEmail("sup@test.com"); sup.setUser(sUser);
        issue.setSupervisior(sup);
        
        Worker w = new Worker(); User wUser = new User(); wUser.setEmail("w@test.com"); w.setUser(wUser);
        issue.setAssigned(Set.of(w));

        MultipartFile file = mock(MultipartFile.class);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        Map<String, String> cloudMap = new HashMap<>();
        cloudMap.put("secure_url", "https://cloudinary.test/img.png");
        cloudMap.put("format", "png");
        when(imageService.saveImage(file, issueId)).thenReturn(cloudMap);

        // Act
        UUID resultId = issueService.doneIssue(issueId, file);

        // Assert
        assertEquals(issueId, resultId);
        assertEquals(Stages.RESOLVED, issue.getStages(), "Issue must be set properly");
        assertNotNull(issue.getResolvedAt(), "Timestamp must be stamped");
        assertEquals("https://cloudinary.test/img.png", issue.getSecureURL(), "Security URL MUST persist");
        verify(issueRepository).save(issue);
        // Expecting 2 events: Worker and Supervisor
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));
    }
}
