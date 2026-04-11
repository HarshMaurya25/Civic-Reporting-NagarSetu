package com.project.nagarSetu.util.dto.user;

import com.project.nagarSetu.util.enums.Roles;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private UUID id;

    private String fullName;

    private String phoneNumber;

    private String email;

    private Roles roles;

    private Boolean enable;

    private Integer age;

    private String gender;

    private String location;

    private LocalDateTime createdAt;
}
