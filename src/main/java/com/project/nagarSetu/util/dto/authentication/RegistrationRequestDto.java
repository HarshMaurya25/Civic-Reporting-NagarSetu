package com.project.nagarSetu.util.dto.authentication;

import com.project.nagarSetu.util.enums.Roles;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequestDto {

    private String fullName;

    private String phoneNumber;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "Name should be atleast 6 letter")
    private String password;

    @NotNull
    private Roles role;

    private String code;

    private Integer age;

    private String gender;

    private String location;
}
