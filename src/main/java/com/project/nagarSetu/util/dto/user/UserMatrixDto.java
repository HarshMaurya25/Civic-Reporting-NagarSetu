package com.project.nagarSetu.util.dto.user;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMatrixDto {

    long reportedIssue;
    long inProgressIssue;
    long resolvedIssue;
}
