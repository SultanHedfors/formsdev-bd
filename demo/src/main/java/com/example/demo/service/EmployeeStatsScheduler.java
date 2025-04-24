package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeStatsScheduler {

//    private final EmployeeStatisticsCalculator calculator;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // co 5 minut
    public void updateDailyStats() {
        log.info("Starting daily stats calculation");
//        calculator.calculateDailyStats();
        log.info("Finished daily stats calculation");
    }

    @Scheduled(cron = "0 0 * * * *") // co godzinę
    public void updateWeeklyStats() {
        log.info("Starting weekly stats calculation");
//        calculator.calculateWeeklyStats();
        log.info("Finished weekly stats calculation");
    }

    @Scheduled(cron = "0 0 3 * * *") // codziennie o 3:00
    public void updateMonthlyAndYearlyStats() {
        log.info("Starting monthly and yearly stats calculation");
//        calculator.calculateMonthlyStats();
//        calculator.calculateYearlyStats();
        log.info("Finished monthly and yearly stats calculation");
    }
}
