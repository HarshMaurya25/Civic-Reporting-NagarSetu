package com.project.nagarSetu.util.dto.issue;

import com.project.nagarSetu.util.enums.Stages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueRecent {
    private UUID id;
    private String title;
    private LocalDateTime createdAt;
    private Stages stages;
    private String imageUrl;
}
