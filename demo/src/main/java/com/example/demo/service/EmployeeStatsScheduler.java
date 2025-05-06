package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
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
    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5 minut
    public void updateDailyStats() {
        LocalDate today = LocalDate.now();  // Używamy bieżącej daty

        log.info("[SCHEDULER] Start daily stats calculation for date: {}", today);

        Map<Integer, Double> stats = calculator.calculateDailyScores(today);  // Używamy metody kalkulacji dla dzisiejszej daty

        stats.forEach((empId, score) ->
                log.info("[SCHEDULER] Daily score for employee {}: {}", empId, score)
        );

        log.info("[SCHEDULER] Finished daily stats calculation for date: {}", today);
    }

    /**
     * Co godzinę: aktualizacja statystyk dla danych sprzed ostatnich 3 lat.
     * Ta metoda uruchamia się co godzinę i oblicza statystyki dla pracowników na podstawie
     * danych sprzed ostatnich 3 lat.
     */
    @Scheduled(cron = "0 0 * * * *") // Co godzinę
    public void updateOldDataStats() {
        LocalDate today = LocalDate.now();
        LocalDate threeYearsAgo = today.minusYears(3);

        log.info("[SCHEDULER] Start calculation for data from the last 3 years: {} to {}", threeYearsAgo, today);

        // Pętla przez wszystkie dni w okresie ostatnich 3 lat
        for (LocalDate date = threeYearsAgo; !date.isAfter(today); date = date.plusDays(1)) {
            final LocalDate finalDate = date;  // Tworzymy finalną zmienną

            Map<Integer, Double> dailyStats = calculator.calculateDailyScores(finalDate);  // Obliczamy statystyki dla każdego dnia

            // Jeżeli brak danych, nie tworzysz pustego rekordu
            if (!dailyStats.isEmpty()) {
                dailyStats.forEach((empId, score) ->
                        log.info("[SCHEDULER] Stats from the last 3 years for employee {} on date {}: {}", empId, finalDate, score)
                );
            }
        }

        // Obliczanie statystyk tygodniowych dla danych sprzed 3 lat
        for (LocalDate date = threeYearsAgo; !date.isAfter(today); date = date.plusWeeks(1)) {
            final LocalDate finalDate = date.with(java.time.DayOfWeek.MONDAY); // Początek tygodnia
            log.info("[SCHEDULER] Start weekly stats calculation for week starting: {}", finalDate);
            calculator.calculateWeeklyScores(finalDate);  // Obliczanie statystyk tygodniowych
        }

        // Obliczanie statystyk miesięcznych dla danych sprzed 3 lat
        for (LocalDate date = threeYearsAgo; !date.isAfter(today); date = date.plusMonths(1)) {
            YearMonth month = YearMonth.from(date);
            log.info("[SCHEDULER] Start monthly stats calculation for month: {}", month);
            calculator.calculateMonthlyScores(month);  // Obliczanie statystyk miesięcznych
        }

        // Obliczanie statystyk rocznych dla danych sprzed 3 lat
        for (int year = threeYearsAgo.getYear(); year <= today.getYear(); year++) {
            log.info("[SCHEDULER] Start yearly stats calculation for year: {}", year);
            calculator.calculateYearlyScores(Year.of(year));  // Obliczanie statystyk rocznych
        }

        log.info("[SCHEDULER] Finished calculation for data from the last 3 years.");
    }

    /**
     * Co tydzień: aktualizacja statystyk tygodniowych dla tygodnia bieżącego.
     * Ta metoda uruchamia się co tydzień (np. co poniedziałek).
     */
    @Scheduled(cron = "0 0 0 * * MON") // Co tydzień (poniedziałek o północy)
    public void updateWeeklyStats() {
        LocalDate today = LocalDate.now();
        log.info("[SCHEDULER] Start weekly stats calculation for week of: {}", today);
        calculator.calculateWeeklyScores(today); // Obliczamy statystyki tygodniowe
        log.info("[SCHEDULER] Finished weekly stats calculation for week of: {}", today);
    }

    /**
     * Codziennie o 3:00: aktualizacja statystyk miesięcznych i rocznych.
     */
    @Scheduled(cron = "0 0 3 * * *")  // Codziennie o 3:00
    public void updateMonthlyAndYearlyStats() {
        YearMonth thisMonth = YearMonth.now();
        log.info("[SCHEDULER] Start monthly stats calculation for month: {}", thisMonth);
        calculator.calculateMonthlyScores(thisMonth);
        log.info("[SCHEDULER] Finished monthly stats calculation for month: {}", thisMonth);

        Year thisYear = Year.now();
        log.info("[SCHEDULER] Start yearly stats calculation for year: {}", thisYear);
        calculator.calculateYearlyScores(thisYear);
        log.info("[SCHEDULER] Finished yearly stats calculation for year: {}", thisYear);
    }
}
