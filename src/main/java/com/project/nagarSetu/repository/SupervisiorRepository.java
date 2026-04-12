package com.project.nagarSetu.repository;

import com.project.nagarSetu.entity.Supervisior;
import com.project.nagarSetu.util.enums.IssueType;
import com.project.nagarSetu.util.dto.admin.AdminUserDto;
import com.project.nagarSetu.util.dto.admin.AdminWorkerDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

@Repository
public interface SupervisiorRepository extends JpaRepository<Supervisior, UUID> {

    @Query("SELECT new com.project.nagarSetu.util.dto.admin.AdminWorkerDto" +
            "(w.id, w.user.fullName, w.user.createdAt, w.user.location, w.started, wd.id, wd.name) " +
            "FROM Supervisior w LEFT JOIN Ward wd ON wd.supervisor.id = w.id")
    List<AdminWorkerDto> findAllSupervisors();

    @Query("SELECT new com.project.nagarSetu.util.dto.admin.AdminWorkerDto" +
            "(w.id, w.user.fullName, w.user.createdAt, w.user.location, w.started, wd.id, wd.name) " +
            "FROM Supervisior w LEFT JOIN Ward wd ON wd.supervisor.id = w.id WHERE w.started = false")
    List<AdminWorkerDto> findAllSupervisorsNoStart();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT s FROM Supervisior s 
        WHERE s.started = true 
        AND (
            6371 * acos(
                cos(radians(:issueLat)) * cos(radians(s.jurisdictionCenterLat)) * 
                cos(radians(s.jurisdictionCenterLon) - radians(:issueLon)) + 
                sin(radians(:issueLat)) * sin(radians(s.jurisdictionCenterLat))
            )
        ) <= s.jurisdictionRadiusKm
        ORDER BY (
            6371 * acos(
                cos(radians(:issueLat)) * cos(radians(s.jurisdictionCenterLat)) * 
                cos(radians(s.jurisdictionCenterLon) - radians(:issueLon)) + 
                sin(radians(:issueLat)) * sin(radians(s.jurisdictionCenterLat))
            )
        ) ASC
        LIMIT 1
        """)
    Optional<Supervisior> findHeadOfArea(
            @Param("issueLat") double issueLat,
            @Param("issueLon") double issueLon,
            @Param("dept") String dept);
}
