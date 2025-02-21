package com.example.demo.service;


import com.example.demo.dto.bsn_logic_dto.StatisticsDto;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    public StatisticsDto getStatistics(String fromDate, String toDate) {
        // TODO: Implement service logic to compute statistics based on the provided dates
        return new StatisticsDto(0);
    }
}
