package com.project.nagarSetu.util.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WardPerformanceDto {
    private String wardName;
    private String supervisorName;
    private double points;
}
