package com.project.nagarSetu.event;

public record IssueAssignedEvent(String email, String subject, String body) {
}