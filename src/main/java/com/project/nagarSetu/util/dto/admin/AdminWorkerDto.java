package com.project.nagarSetu.util.dto.admin;

import lombok.*;

import com.project.nagarSetu.util.enums.IssueType;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AdminWorkerDto {
    private UUID id;
    private String username;
    private LocalDateTime createdAt;
    private String location;
    private Boolean started;
    private UUID wardId;
    private String wardName;
}
