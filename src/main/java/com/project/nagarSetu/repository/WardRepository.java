package com.project.nagarSetu.repository;

import com.project.nagarSetu.entity.Ward;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WardRepository extends JpaRepository<Ward, UUID> {

    Optional<Ward> findByNameIgnoreCase(String name);

    long deleteByNameIgnoreCase(String name);

    // Find ward containing the given point
    @Query("SELECT w FROM Ward w WHERE within(:point, w.boundary) = true")
    Optional<Ward> findWardByLocation(@Param("point") Point point);

    // Find wards intersecting with the given bounding box polygon
    @Query("SELECT w FROM Ward w WHERE intersects(w.boundary, :polygon) = true")
    List<Ward> findWardsWithinBoundingBox(@Param("polygon") Polygon polygon);

    Optional<Ward> findBySupervisor_Id(UUID supervisorId);
}
