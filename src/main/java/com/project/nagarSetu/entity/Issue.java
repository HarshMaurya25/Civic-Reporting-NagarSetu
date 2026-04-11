package com.project.nagarSetu.entity;

import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.IssueType;
import com.project.nagarSetu.util.enums.Stages;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE issue SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Criticality criticality;

    @Column(nullable = false)
    private String location;

    // Normalized administrative area key for stable analytics slices.
    private String wardId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Stages stages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;

    @Column(nullable = false)
    private LocalDateTime createAt;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime assignedAt;

    private LocalDateTime inProgressAt;

    private LocalDateTime firstResponseAt;

    private String secureURL;

    private String format;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "issues")
    private Set<Worker> assigned;

    @ManyToOne(fetch = FetchType.LAZY)
    private Supervisior supervisior;

    private LocalDateTime resolvedAt;

    private Integer targetResolutionMinutes;

    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private boolean slaBreached = false;

    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private boolean reopened = false;

    @Column(columnDefinition = "integer default 0")
    @Builder.Default
    private Integer reopenCount = 0;

    private Integer citizenFeedbackScore;

    private boolean admin;

    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void atCreate() {
        this.createAt = LocalDateTime.now();
        this.stages = Stages.PENDING;
        this.admin = false;
        if (this.reopenCount == null) {
            this.reopenCount = 0;
        }
    }
}
