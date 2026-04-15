package com.project.nagarSetu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Polygon;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "wards",
    indexes = {
        @Index(name = "idx_ward_boundary", columnList = "boundary")
    }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    // Keep nullable for existing DB rows when ddl-auto=update.
    @Column(nullable = true)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon boundary;

    @Column(nullable = true)
    private String region;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", unique = true)
    private Supervisior supervisor;

    @PrePersist
    public void atCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        normalizeData();
    }

    @PreUpdate
    public void normalizeData() {
        if (this.name != null) {
            this.name = this.name.trim().toUpperCase();
        }
        if (this.region != null) {
            this.region = this.region.trim();
        }
    }
}
