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
                        "(w.id , w.user.fullName ,w.user.createdAt , w.user.location , w.started , w.department) FROM Supervisior w")
        List<AdminWorkerDto> findAllWorker();

        @Query("SELECT new com.project.nagarSetu.util.dto.admin.AdminWorkerDto" +
                        "(w.id , w.user.fullName ,w.user.createdAt , w.user.location , w.started , w.department)" +
                        " FROM Supervisior w WHERE w.started = false")
        List<AdminWorkerDto> findAllWorkerNoStart();

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
        List<Supervisior> findByUserLocationAndDepartment(String location, IssueType department);

        @Query(value = """
            SELECT * FROM supervisior s 
            WHERE s.department = :dept 
            AND s.jurisdiction_radius_km IS NOT NULL 
            AND s.jurisdiction_center_lat IS NOT NULL
            AND (6371 * acos(cos(radians(:issueLat)) * cos(radians(s.jurisdiction_center_lat)) * 
                 cos(radians(s.jurisdiction_center_lon) - radians(:issueLon)) + 
                 sin(radians(:issueLat)) * sin(radians(s.jurisdiction_center_lat)))) <= s.jurisdiction_radius_km
            ORDER BY (6371 * acos(cos(radians(:issueLat)) * cos(radians(s.jurisdiction_center_lat)) * 
                 cos(radians(s.jurisdiction_center_lon) - radians(:issueLon)) + 
                 sin(radians(:issueLat)) * sin(radians(s.jurisdiction_center_lat)))) ASC LIMIT 1
        """, nativeQuery = true)
        Optional<Supervisior> findHeadOfArea(@Param("issueLat") double lat, @Param("issueLon") double lon, @Param("dept") String dept);
}
