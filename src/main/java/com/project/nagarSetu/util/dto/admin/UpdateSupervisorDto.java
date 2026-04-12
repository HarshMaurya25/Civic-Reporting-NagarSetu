package com.project.nagarSetu.util.dto.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSupervisorDto {
    private String fullName;
    private String phoneNumber;
    private Integer age;
    private String gender;
    private String location;
    private String jurisdictionName;
}
