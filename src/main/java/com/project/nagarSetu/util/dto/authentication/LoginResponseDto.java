package com.project.nagarSetu.util.dto.authentication;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private UUID id;
    private String email;
    private String fullName;
    private String token;
}