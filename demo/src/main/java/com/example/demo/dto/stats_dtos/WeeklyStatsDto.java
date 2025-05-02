package com.example.demo.dto.stats_dtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatsDto {
    private LocalDate weekStart;
    private Double score;
    private LocalDateTime updatedAt;
}