package com.project.nagarSetu.util.dto.publicdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NameDateDto {
    private UUID id;
    private String name;
    private LocalDateTime date;
}

