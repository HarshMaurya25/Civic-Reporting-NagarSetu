package com.project.nagarSetu.repository;

import com.project.nagarSetu.entity.Worker;
import com.project.nagarSetu.util.dto.admin.AdminUserDto;
import com.project.nagarSetu.util.dto.user.GetWorkerForSupervisorDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    @Query("SELECT new com.project.nagarSetu.util.dto.admin.AdminUserDto(" +
            "w.id, u.fullName, u.createdAt, u.location, w.started, " +
            "s.id, su.fullName, wd.id, wd.name) " +
            "FROM Worker w " +
            "LEFT JOIN w.user u " +
            "LEFT JOIN w.supervisior s " +
            "LEFT JOIN s.user su " +
            "LEFT JOIN Ward wd ON wd.supervisor.id = s.id")
    List<AdminUserDto> findAllWorker();

    @Query("SELECT new com.project.nagarSetu.util.dto.admin.AdminUserDto(" +
            "w.id, u.fullName, u.createdAt, u.location, w.started, " +
            "s.id, su.fullName, wd.id, wd.name) " +
            "FROM Worker w " +
            "LEFT JOIN w.user u " +
            "LEFT JOIN w.supervisior s " +
            "LEFT JOIN s.user su " +
            "LEFT JOIN Ward wd ON wd.supervisor.id = s.id " +
            "WHERE w.started = false")
    List<AdminUserDto> findAllWorkerNoStart();

    @Query("SELECT new com.project.nagarSetu.util.dto.admin.WorkerAssignmentDto(w.id, w.user.fullName, COUNT(i)) " +
            "FROM Worker w LEFT JOIN w.issues i " +
            "WHERE w.supervisior.id = :supervisorId " +
            "GROUP BY w.id, w.user.fullName ORDER BY COUNT(i) ASC")
    List<com.project.nagarSetu.util.dto.admin.WorkerAssignmentDto> findWorkersWithLoad(
            @Param("supervisorId") UUID supervisorId);

    List<Worker> findBySupervisior_Id(UUID supervisiorId);

    long countBySupervisior_Id(UUID supervisiorId);

    @Query("""
                SELECT new com.project.nagarSetu.util.dto.user.GetWorkerForSupervisorDto(
                    w.id,
                    w.user.fullName,
                    COALESCE(SUM(CASE WHEN i.stages IN (
                        com.project.nagarSetu.util.enums.Stages.ACKNOWLEDGED,
                        com.project.nagarSetu.util.enums.Stages.TEAM_ASSIGNED,
                        com.project.nagarSetu.util.enums.Stages.IN_PROGRESS
                    ) THEN 1 ELSE 0 END), 0)
                )
                FROM Worker w
                LEFT JOIN w.issues i
                WHERE w.supervisior.id = :id
                GROUP BY w.id, w.user.fullName
            """)
    List<GetWorkerForSupervisorDto> findTheWorker(@Param("id") UUID id);

    @Query("""
            SELECT w
            FROM Worker w
            JOIN w.issues i
            WHERE i.id = :issueId
            """)
    Worker findWorkersByIssueId(@Param("issueId") UUID issueId);

    @Query("""
                SELECT new com.project.nagarSetu.util.dto.issue.WorkerScoringDto(
                   w.id,
                   w.user.fullName,
                   w.latitude,
                   w.longitude,
                   w.lastAssignedAt,
                   w.lifetimeAssignments,
                   (SELECT COUNT(i) FROM w.issues i WHERE i.stages != 'RESOLVED' AND i.isDeleted = false)
                )
                FROM Worker w
                WHERE w.supervisior.id = :supervisorId
                AND w.started = true
            """)
    List<com.project.nagarSetu.util.dto.issue.WorkerScoringDto> findScorableWorkers(
            @Param("supervisorId") UUID supervisorId);

}
