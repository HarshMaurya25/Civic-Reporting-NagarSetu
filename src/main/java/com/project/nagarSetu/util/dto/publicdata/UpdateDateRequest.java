package com.project.nagarSetu.util.dto.publicdata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDateRequest {
    /**
     * Supported entity values: WORKER, SUPERVISOR, WARD, ISSUE
     */
    private String entity;
    private UUID id;
    /**
     * For ISSUE supported fields: createAt, acknowledgedAt, assignedAt, inProgressAt, firstResponseAt, resolvedAt
     * For WORKER/SUPERVISOR supported field: createdAt (updates underlying User.createdAt)
     * For WARD supported field: createdAt
     */
    private String field;
    private LocalDateTime value;
}

