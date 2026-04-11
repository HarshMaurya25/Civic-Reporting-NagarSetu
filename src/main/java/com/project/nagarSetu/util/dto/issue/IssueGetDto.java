package com.project.nagarSetu.util.dto.issue;

import com.project.nagarSetu.entity.User;
import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.IssueType;
import com.project.nagarSetu.util.enums.Stages;
import jakarta.persistence.*;
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
public class IssueGetDto {
    private UUID id;

    private String title;

    private IssueType issueType;

    private String description;

    private Criticality criticality;

    private String location;

    private double latitude;

    private double longitude;

    private Stages stages;

    private String submittedBy;

    private LocalDateTime createAt;

    private String imageUrl;
}
