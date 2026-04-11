package com.project.nagarSetu.util.dto.issue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class IssueDailyCountDto {
    private LocalDate date;
    private long count;

    // Hibernate may return java.sql.Date for FUNCTION('date', ...)
    public IssueDailyCountDto(java.sql.Date date, Long count) {
        this.date = date == null ? null : date.toLocalDate();
        this.count = count == null ? 0L : count;
    }

    // Hibernate might also return java.time.LocalDate
    public IssueDailyCountDto(java.time.LocalDate date, Long count) {
        this.date = date;
        this.count = count == null ? 0L : count;
    }

    // Or java.time.LocalDateTime (take only the date part)
    public IssueDailyCountDto(java.time.LocalDateTime dateTime, Long count) {
        this.date = dateTime == null ? null : dateTime.toLocalDate();
        this.count = count == null ? 0L : count;
    }

    // Or java.util.Date (covers various dialect returns)
    public IssueDailyCountDto(java.util.Date utilDate, Long count) {
        if (utilDate == null) {
            this.date = null;
        } else {
            java.time.Instant instant = utilDate.toInstant();
            this.date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        this.count = count == null ? 0L : count;
    }

    // Or java.sql.Timestamp
    public IssueDailyCountDto(java.sql.Timestamp timestamp, Long count) {
        this.date = timestamp == null ? null : timestamp.toLocalDateTime().toLocalDate();
        this.count = count == null ? 0L : count;
    }
}