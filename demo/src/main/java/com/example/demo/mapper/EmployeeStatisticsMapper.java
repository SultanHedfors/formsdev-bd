package com.example.demo.mapper;

import com.example.demo.dto.stats_dtos.DailyStatsDto;
import com.example.demo.dto.stats_dtos.MonthlyStatsDto;
import com.example.demo.dto.stats_dtos.WeeklyStatsDto;
import com.example.demo.dto.stats_dtos.YearlyStatsDto;
import com.example.demo.entity.stats.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeStatisticsMapper {

    @Mapping(source="startDay", target="date")
    DailyStatsDto toDailyDto(EmployeeDailyStatsEntity e);
    List<DailyStatsDto> toDailyDtoList(List<EmployeeDailyStatsEntity> l);

    @Mapping(source="weekStart", target="weekStart")
    WeeklyStatsDto toWeeklyDto(EmployeeWeeklyStatsEntity e);
    List<WeeklyStatsDto> toWeeklyDtoList(List<EmployeeWeeklyStatsEntity> l);

    @Mapping(source="month", target="month")
    MonthlyStatsDto toMonthlyDto(EmployeeMonthlyStatsEntity e);
    List<MonthlyStatsDto> toMonthlyDtoList(List<EmployeeMonthlyStatsEntity> l);

    @Mapping(source="year", target="year")
    YearlyStatsDto toYearlyDto(EmployeeYearlyStatsEntity e);
    List<YearlyStatsDto> toYearlyDtoList(List<EmployeeYearlyStatsEntity> l);
}