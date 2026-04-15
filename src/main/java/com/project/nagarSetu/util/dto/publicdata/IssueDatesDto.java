package com.project.nagarSetu.util.dto.publicdata;

import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueDatesDto {
    private UUID id;
    private String title;
    private Stages stage;
    private LocalDateTime createAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime inProgressAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
}

