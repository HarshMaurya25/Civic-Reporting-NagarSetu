package com.project.nagarSetu.util.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class WorkerAssignmentDto {
    private UUID id;
    private String fullName;
    private Long assignedCount;
}