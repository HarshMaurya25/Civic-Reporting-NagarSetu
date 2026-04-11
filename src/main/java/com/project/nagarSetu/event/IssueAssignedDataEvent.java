package com.project.nagarSetu.event;

import java.util.UUID;

public record IssueAssignedDataEvent(
        String recipientEmail,
        String recipientRole,
        UUID issueId,
        String title,
        String description,
        String criticality,
        String url,
        String workerName,
        UUID workerId,
        String supervisorName) {
}