package com.project.nagarSetu.util.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardDetailDto {
    private UUID wardId;
    private String wardNumber;
    private String supervisorName;
    private String supervisorEmail;
    private String supervisorPhoneNumber;
    private Long workerCount;
    private Long unfinishedIssueCount;
    private String regionName;
}
