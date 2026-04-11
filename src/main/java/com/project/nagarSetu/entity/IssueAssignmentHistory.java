package com.project.nagarSetu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    private UUID fromWorkerId;

    private UUID toWorkerId;

    private UUID supervisorId;

    @Column(nullable = false)
    private String action;

    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void atCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
