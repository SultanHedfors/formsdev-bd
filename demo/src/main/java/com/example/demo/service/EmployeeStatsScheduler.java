package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeStatsScheduler {

    private final EmployeeStatisticsCalculator calculator;

    @Scheduled(fixedDelay = 60000) // co 10 sekund dla testów
    public void updateDailyStats() {
        LocalDate date = LocalDate.of(2025, 1, 1);

        log.info("[SCHEDULER] Start daily stats calculation for date: {}", date);

        Map<Integer, Double> stats = calculator.calculateDailyScores(date);

        for (Map.Entry<Integer, Double> entry : stats.entrySet()) {
            Integer employeeId = entry.getKey();
            Double score = entry.getValue();
            log.info("[SCHEDULER] Daily score for employee {}: {}", employeeId, score);
        }

        log.info("[SCHEDULER] Finished daily stats calculation for date: {}", date);
    }

//    @Scheduled(cron = "0 0 * * * *") // co godzinę
//    public void updateWeeklyStats() {
//        log.info("Starting weekly stats calculation");
////        calculator.calculateWeeklyStats();
//        log.info("Finished weekly stats calculation");
//    }
//
//    @Scheduled(cron = "0 0 3 * * *") // codziennie o 3:00
//    public void updateMonthlyAndYearlyStats() {
//        log.info("Starting monthly and yearly stats calculation");
////        calculator.calculateMonthlyStats();
////        calculator.calculateYearlyStats();
//        log.info("Finished monthly and yearly stats calculation");
//    }
}
