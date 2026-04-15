package com.project.nagarSetu.util.dto.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SimpleWorkerDto {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String location;
    private Boolean started;
    private LocalDateTime createdAt;
    private UUID supervisorId;
    private String supervisorName;
}
