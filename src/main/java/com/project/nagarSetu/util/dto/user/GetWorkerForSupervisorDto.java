package com.project.nagarSetu.util.dto.user;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetWorkerForSupervisorDto {
    private UUID id;

    private String fullName;

    private Long issueCount;
}
