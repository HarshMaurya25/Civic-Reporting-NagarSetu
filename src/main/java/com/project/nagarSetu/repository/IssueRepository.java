package com.project.nagarSetu.repository;

import com.project.nagarSetu.entity.Issue;
import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.util.dto.issue.IssueByMap;
import com.project.nagarSetu.util.dto.issue.IssueGetByUserDto;
import com.project.nagarSetu.util.dto.issue.IssueGetDto;
import com.project.nagarSetu.util.dto.issue.IssueRecent;
import com.project.nagarSetu.util.dto.issue.IssueForWorkerDto;
import com.project.nagarSetu.util.dto.user.UserLeaderboardDto;
import com.project.nagarSetu.util.dto.user.UserMatrixDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueGetDto(" +
                        "i.id , i.title , i.issueType , i.description , i.criticality , i.location , i.latitude , i.longitude,"
                        +
                        "i.stages , i.submittedBy.fullName , i.createAt , i.secureURL " +
                        ") FROM Issue i WHERE i.id = :id")
        IssueGetDto getIssueById(@Param("id") UUID id);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueGetByUserDto(" +
                        "i.id , i.title , i.location , i.stages , i.createAt , i.criticality , i.issueType" +
                        ") FROM Issue i WHERE i.submittedBy.id = :id ORDER BY COALESCE(i.resolvedAt, i.createAt) DESC")
        Page<IssueGetByUserDto> getIssueByUser(@Param("id") UUID id, Pageable pageable);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueByMap(" +
                        "i.id , i.latitude , i.longitude , i.criticality , i.stages, i.issueType" +
                        ") FROM Issue i WHERE i.submittedBy.id = :id AND " +
                        "(i.stages <> 'RESOLVED' OR (i.resolvedAt IS NOT NULL AND i.resolvedAt >= :oneMonthAgo))")
        Set<IssueByMap> getIssueMapByUserId(@Param("id") UUID id, @Param("oneMonthAgo") LocalDateTime oneMonthAgo);

        @Query("SELECT new com.project.nagarSetu.util.dto.user.UserMatrixDto(" +
                        "COUNT(CASE WHEN i.stages = 'PENDING' THEN 1 END)," +
                        "COUNT(CASE WHEN i.stages IN ('PENDING', 'ACKNOWLEDGED', 'TEAM_ASSIGNED', 'IN_PROGRESS') THEN 1 END), "
                        +
                        "COUNT(CASE WHEN i.stages = 'RESOLVED' THEN 1 END)" +
                        ") FROM Issue i WHERE i.submittedBy.id = :id")
        UserMatrixDto getIssueMatrix(@Param("id") UUID id);

        @Query("SELECT new com.project.nagarSetu.util.dto.user.UserLeaderboardDto(" +
                        "  u.fullName, " +
                        "  COUNT(CASE WHEN i.stages IN ('PENDING', 'ACKNOWLEDGED', 'TEAM_ASSIGNED', 'IN_PROGRESS', 'RESOLVED') THEN 1 END) "
                        +
                        ") " +
                        "FROM Issue i " +
                        "JOIN i.submittedBy u " +
                        "GROUP BY u.id " +
                        "ORDER BY COUNT(CASE WHEN i.stages IN ('PENDING', 'ACKNOWLEDGED', 'TEAM_ASSIGNED', 'IN_PROGRESS', 'RESOLVED') THEN 1 END) DESC")
        List<UserLeaderboardDto> getLeaderBoard(Pageable pageable);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueGetByUserDto(" +
                        "  i.id, i.title, i.location, i.stages, i.createAt, i.criticality, i.issueType" +
                        ") " +
                        "FROM Issue i " +
                        "WHERE i.location = :location " +
                        "  AND i.stages = :stages " +
                        "  AND i.issueType = :type ORDER BY COALESCE(i.resolvedAt, i.createAt) DESC")
        Page<IssueGetByUserDto> getIssueByLocationStageAndType(String location, String stages, String type,
                        Pageable pageable);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueForWorkerDto(" +
                        "i.id, i.title, i.description, i.stages, i.createAt, i.secureURL, i.criticality, i.issueType, "
                        +
                        "COALESCE(i.supervisior.user.fullName, '') ) " +
                        "FROM Issue i JOIN i.assigned w " +
                        "WHERE w.id = :workerId AND i.stages != 'RESOLVED' " +
                        "ORDER BY i.createAt DESC")
        java.util.List<IssueForWorkerDto> getAssignedOpenIssues(java.util.UUID workerId);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueRecent(" +
                        "i.id , i.title , i.createAt , i.stages , i.secureURL" +
                        ") FROM Issue i ORDER BY COALESCE(i.resolvedAt, i.createAt) DESC")
        Page<IssueRecent> getRecentIssue(Pageable pageable);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueStageCountDto(i.stages, COUNT(i)) " +
                        "FROM Issue i WHERE i.createAt >= :since GROUP BY i.stages")
        java.util.List<com.project.nagarSetu.util.dto.issue.IssueStageCountDto> getIssueCountSinceGroupedByStage(
                        @Param("since") LocalDateTime since);

        @Query("SELECT FUNCTION('date', i.createAt), COUNT(i) " +
                        "FROM Issue i WHERE i.createAt >= :since GROUP BY FUNCTION('date', i.createAt) ORDER BY FUNCTION('date', i.createAt) ASC")
        java.util.List<Object[]> getCreatedCountSinceGroupedByDate(@Param("since") java.time.LocalDateTime since);

        @Query("SELECT FUNCTION('date', i.resolvedAt), COUNT(i) " +
                        "FROM Issue i WHERE i.resolvedAt IS NOT NULL AND i.resolvedAt >= :since GROUP BY FUNCTION('date', i.resolvedAt) ORDER BY FUNCTION('date', i.resolvedAt) ASC")
        java.util.List<Object[]> getResolvedCountSinceGroupedByDate(@Param("since") java.time.LocalDateTime since);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueByMap(" +
                        "i.id, i.latitude, i.longitude, i.criticality, i.stages, i.issueType) " +
                        "FROM Issue i WHERE i.location = :location AND i.issueType = :department")
        java.util.Set<IssueByMap> getIssueMapForSupervisor(@Param("location") String location,
                        @Param("department") com.project.nagarSetu.util.enums.IssueType department);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueByMap(" +
                        "i.id, i.latitude, i.longitude, i.criticality, i.stages, i.issueType) " +
                        "FROM Issue i JOIN i.assigned w WHERE w.id = :workerId AND i.stages != 'RESOLVED'")
        java.util.Set<IssueByMap> getAssignedOpenIssueMap(@Param("workerId") java.util.UUID workerId);

        @Query("SELECT new com.project.nagarSetu.util.dto.issue.IssueByMap(" +
                        "i.id, i.latitude, i.longitude, i.criticality, i.stages, i.issueType) " +
                        "FROM Issue i")
        java.util.Set<IssueByMap> getAllIssueMap();

    @Query("""
    SELECT w FROM Worker w
    JOIN w.issues i
    WHERE i.id = :issueId
""")
        List<Worker> findWorkersByIssueId(@Param("issueId") UUID issueId);

        List<Issue> findBySupervisiorIdAndAdminTrue(UUID supervisiorId);
}
