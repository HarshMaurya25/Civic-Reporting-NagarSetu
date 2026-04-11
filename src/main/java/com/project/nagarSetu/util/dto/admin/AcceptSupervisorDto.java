package com.project.nagarSetu.util.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcceptSupervisorDto {
    private String department;
    private String jurisdictionName;
    private Double jurisdictionCenterLat;
    private Double jurisdictionCenterLon;
    private Double jurisdictionRadiusKm;
}
