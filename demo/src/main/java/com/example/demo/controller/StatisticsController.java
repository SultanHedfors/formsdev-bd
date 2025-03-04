package com.example.demo.controller;


import com.example.demo.dto.bsn_logic_dto.StatisticsDto;
import com.example.demo.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<StatisticsDto> getStatistics(@RequestParam(required = false) String fromDate,
                                                       @RequestParam(required = false) String toDate) {
        StatisticsDto statistics = statisticsService.getStatistics(fromDate, toDate);
        return ResponseEntity.ok(statistics);
    }
}