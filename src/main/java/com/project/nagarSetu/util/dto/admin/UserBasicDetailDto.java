package com.project.nagarSetu.util.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicDetailDto {
    private String fullName;
    private String email;
    private String phoneNumber;
    private Integer age;
}
