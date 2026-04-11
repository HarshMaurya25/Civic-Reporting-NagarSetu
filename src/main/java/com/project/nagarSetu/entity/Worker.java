package com.project.nagarSetu.entity;

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
public class Worker {

    @Id
    private UUID id;

    @OneToOne
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisior_id")
    private Supervisior supervisior;

    private Boolean started;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "worker_issues",
            joinColumns = @JoinColumn(name = "worker_id"),
            inverseJoinColumns = @JoinColumn(name = "issue_id")
    )
    private Set<Issue> issues;

    private LocalDateTime lastAssignedAt;

    @Column(columnDefinition = "integer default 0")
    @Builder.Default
    private Integer lifetimeAssignments = 0;

    private Double latitude;
    private Double longitude;

    @PrePersist
    private void atCreate(){
        started = false;
        lifetimeAssignments = 0;
    }

}
