package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeStatsScheduler {

    private final EmployeeStatisticsCalculator calculator;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void updateDailyStats() {
        LocalDate today = LocalDate.now();
        log.info("[SCHEDULER] Start daily stats calculation for date: {}", today);
        calculator.calculateDailyScores(today).forEach((empId, score) ->
                log.debug("[SCHEDULER] Daily score for employee {}: {}", empId, score)
        );
        log.info("[SCHEDULER] Finished daily stats calculation for date: {}", today);
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void updateOldDataStats() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusYears(3);
        log.info("[SCHEDULER] Start calculation for last 3 years: {} to {}", from, today);

        // Daily
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            calculator.calculateDailyScores(d);
        }

        // Weekly
        for (LocalDate d = from.with(DayOfWeek.MONDAY); !d.isAfter(today); d = d.plusWeeks(1)) {
            log.debug("[SCHEDULER] Weekly stats for week starting: {}", d);
            calculator.calculateWeeklyScores(d);
        }

        // Monthly
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(today)); m = m.plusMonths(1)) {
            log.debug("[SCHEDULER] Monthly stats for month: {}", m);
            calculator.calculateMonthlyScores(m);
        }

        // Yearly
        for (int y = from.getYear(); y <= today.getYear(); y++) {
            log.debug("[SCHEDULER] Yearly stats for year: {}", y);
            calculator.calculateYearlyScores(Year.of(y));
        }

        log.info("[SCHEDULER] Finished calculation for last 3 years.");
    }

}
