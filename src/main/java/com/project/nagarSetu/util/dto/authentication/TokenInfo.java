package com.project.nagarSetu.util.dto.authentication;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenInfo {
    private String id;
    private String roles;
    private Date expirationAt;
}
