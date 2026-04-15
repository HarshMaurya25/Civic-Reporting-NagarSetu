package com.project.nagarSetu.util.dto.issue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WardMatrixRowDto {
    private UUID wardId;
    private String wardName;
    private long reported;
    private long solved;
    private long inBetween;
    private long slaBreached;
}

