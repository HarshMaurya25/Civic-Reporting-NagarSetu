package com.project.nagarSetu.util.dto.worker;
import com.project.nagarSetu.util.enums.Roles;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkerCreateResponse {
    private UUID id;
    private String token;
    private Roles roles;
    private boolean started;
}
