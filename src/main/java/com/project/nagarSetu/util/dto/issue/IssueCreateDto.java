package com.project.nagarSetu.util.dto.issue;

import java.util.UUID;

import com.project.nagarSetu.util.enums.Criticality;
import com.project.nagarSetu.util.enums.IssueType;
import com.project.nagarSetu.util.enums.Stages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCreateDto {

    @NotNull(message = "Title is required")
    private String title;

    @NotNull(message = "Issue type is required")
    private IssueType issueType;

    @NotNull(message = "Description is required")
    private String description;

    @NotNull(message = "Criticality is required")
    private Criticality criticality;

    @NotNull(message = "Location is required")
    private String location;

    private String wardId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "SubmittedBy (user ID) is required")
    private UUID submittedById;

    private Integer targetResolutionMinutes;
}
