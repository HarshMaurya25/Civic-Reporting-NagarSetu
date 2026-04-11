package com.project.nagarSetu.util.dto.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserDto {
    private UUID id;
    private String username;
    private LocalDateTime createdAt;
    private String location;
    private Boolean started;
}
