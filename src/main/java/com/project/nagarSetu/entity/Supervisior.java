package com.project.nagarSetu.entity;

import com.project.nagarSetu.util.enums.IssueType;
import com.project.nagarSetu.util.enums.Roles;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Supervisior {
    @Id
    private UUID id;

    @OneToOne
    private User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "supervisior")
    private Set<Worker> worker;

    @Enumerated(EnumType.STRING)
    private IssueType department;

    private Boolean started;

    private java.util.UUID lastAssignedWorkerId;

    private Double jurisdictionCenterLat;
    private Double jurisdictionCenterLon;
    private Double jurisdictionRadiusKm;
    private String jurisdictionName;

    @Column(columnDefinition = "integer default 0")
    @Builder.Default
    private Integer totalAssignmentsDispatched = 0;

    @PrePersist
    private void atCreate() {
        started = false;
        totalAssignmentsDispatched = 0;
    }
}