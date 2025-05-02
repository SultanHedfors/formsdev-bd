package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeStatsScheduler {

    private final EmployeeStatisticsCalculator calculator;

    /**
     * Co 5 minut: aktualizacja statystyk dziennych dla dzisiejszej daty.
     * Save() z JPA zrobi update istniejącego wiersza jeśli taki PK już jest,
     * inaczej wstawi nowy.
     */
    @Scheduled(fixedDelay = 1000)
    public void updateDailyStats() {
//        #TODO dla testow wartosc ze stycznia bo taki jest wczytany grafik
        LocalDate today = returnDate();

        log.info("[SCHEDULER] Start daily stats calculation for date: {}", today);

        Map<Integer, Double> stats = calculator.calculateDailyScores(today);

        stats.forEach((empId, score) ->
                log.info("[SCHEDULER] Daily score for employee {}: {}", empId, score)
        );

        log.info("[SCHEDULER] Finished daily stats calculation for date: {}", today);
    }

    /**
     * Co godzinę: aktualizacja statystyk tygodniowych.
     * Jako okres wybieramy tydzień zaczynający się od poniedziałku bieżącego tygodnia.
     */
//    @Scheduled(cron = "0 0 * * * *")
    @Scheduled(fixedDelay = 1000)
    public void updateWeeklyStats() {
        LocalDate today = returnDate();
        log.info("[SCHEDULER] Start weekly stats calculation for week of: {}", today);
        calculator.calculateWeeklyScores(today);
        log.info("[SCHEDULER] Finished weekly stats calculation for week of: {}", today);
    }

    /**
     * Codziennie o 3:00: aktualizacja statystyk miesięcznych i rocznych.
     */
//    @Scheduled(cron = "0 0 3 * * *")
//    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void updateMonthlyAndYearlyStats() {
        YearMonth thisMonth = YearMonth.of(2025,1);
        log.info("[SCHEDULER] Start monthly stats calculation for month: {}", thisMonth);
        calculator.calculateMonthlyScores(thisMonth);
        log.info("[SCHEDULER] Finished monthly stats calculation for month: {}", thisMonth);

        Year thisYear = Year.now();
        log.info("[SCHEDULER] Start yearly stats calculation for year: {}", thisYear);
        calculator.calculateYearlyScores(thisYear);
        log.info("[SCHEDULER] Finished yearly stats calculation for year: {}", thisYear);
    }

    private LocalDate returnDate(){
        return LocalDate.of(2025,1,7);
    }
}
