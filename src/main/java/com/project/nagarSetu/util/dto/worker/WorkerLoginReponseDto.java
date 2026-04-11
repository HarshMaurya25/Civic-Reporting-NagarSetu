package com.project.nagarSetu.util.dto.worker;

import java.util.UUID;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkerLoginReponseDto {
    private UUID id;
    private String email;
    private String fullName;
    private String token;
    private Boolean started;
}
