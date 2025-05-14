package com.example.demo.dto.stats_dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatsDto {
    private String employeeName;
    private String employeeCode;
    private boolean currentUser;
    private List<DailyStatsDto>   dailyStats;
    private List<WeeklyStatsDto>  weeklyStats;
    private List<MonthlyStatsDto> monthlyStats;
    private List<YearlyStatsDto>  yearlyStats;
}
