package com.example.demo.dto.bsn_logic_dto;

import com.example.demo.dto.stats_dtos.EmployeeStatsDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDto {
    private List<EmployeeStatsDto> employees;
}