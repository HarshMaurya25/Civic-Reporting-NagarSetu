package com.project.nagarSetu.util.dto.issue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Issue30DayStatsDto {
    private List<IssueDailyCountDto> created;
    private List<IssueDailyCountDto> resolved;
}