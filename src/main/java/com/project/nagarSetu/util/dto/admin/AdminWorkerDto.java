package com.project.nagarSetu.util.dto.admin;

import lombok.*;

import com.project.nagarSetu.util.enums.IssueType;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class AdminWorkerDto {
    private UUID id;
    private String username;
    private LocalDateTime createdAt;
    private String location;
    private Boolean started;
    private IssueType department;

    public AdminWorkerDto(UUID id, String username, LocalDateTime createdAt, String location, Boolean started,
            IssueType department) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
        this.location = location;
        this.started = started;
        this.department = department;
    }
}
