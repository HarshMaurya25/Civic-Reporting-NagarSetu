package com.project.nagarSetu.entity;

import com.project.nagarSetu.util.enums.Stages;
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
public class IssueStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stages fromStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stages toStage;

    @Column(nullable = false)
    private String actorType;

    private UUID actorId;

    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void atCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
