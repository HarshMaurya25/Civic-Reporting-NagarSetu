package com.project.nagarSetu.util.dto.authentication;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {
    private UUID id;
    private String token;
}
