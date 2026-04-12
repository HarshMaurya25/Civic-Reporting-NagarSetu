package com.project.nagarSetu.util.dto.admin;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminWardDto {
    private UUID wardId;
    private String wardName;
    private String boundary;
    private String region;
    private UUID supervisorId;
    private String supervisorName;
}
