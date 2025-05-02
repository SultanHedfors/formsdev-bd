package com.example.demo.service;

import com.example.demo.entity.ActivityEmployeeEntity;
import com.example.demo.entity.ProcedureEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.entity.stats.*;
import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.stats.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeStatisticsCalculator {
    private final ActivityEmployeeRepository activityRepo;
    private final DailyEmployeeStatisticRepository dailyRepo;
    private final WeeklyEmployeeStatisticRepository weeklyRepo;
    private final MonthlyEmployeeStatisticRepository monthlyRepo;
    private final YearlyEmployeeStatisticRepository yearlyRepo;

    public Map<Integer, Double> calculateDailyScores(LocalDate date) {
        return calculateAndSave(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                date,
                (empId, score) -> new EmployeeDailyStatsEntity(empId, date, score, LocalDateTime.now()),
                dailyRepo::save
        );
    }

    public void calculateWeeklyScores(LocalDate anyDate) {
        LocalDate monday = anyDate.with(DayOfWeek.MONDAY);
        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = start.plusWeeks(1);
        calculateAndSave(
                start, end, monday,
                (empId, score) -> new EmployeeWeeklyStatsEntity(empId, monday, score, LocalDateTime.now()),
                weeklyRepo::save
        );
    }

    public void calculateMonthlyScores(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        calculateAndSave(
                start, end, month.toString(),
                (empId, score) -> new EmployeeMonthlyStatsEntity(empId, month.toString(), score, LocalDateTime.now()),
                monthlyRepo::save
        );
    }

    public void calculateYearlyScores(Year year) {
        LocalDateTime start = year.atDay(1).atStartOfDay();
        LocalDateTime end = start.plusYears(1);
        calculateAndSave(
                start, end, year.getValue(),
                (empId, score) -> new EmployeeYearlyStatsEntity(empId, year.getValue(), score, LocalDateTime.now()),
                yearlyRepo::save
        );
    }

    /**
     * Wspólna logika: zbiera wszystkie wpisy, filtruje, grupuje, liczy score i zapisuje.
     */
    private <T> Map<Integer, Double> calculateAndSave(
            LocalDateTime start,
            LocalDateTime end,
            Object periodKey,
            BiFunction<Integer, Double, T> entityFactory,
            java.util.function.Consumer<T> saver
    ) {
        log.info("Calculating stats for period starting {} to {} (key={})", start, end, periodKey);

        // pobierz i odfiltruj
        List<ActivityEmployeeEntity> raw = activityRepo.findWithGraphByActivityDate(start, end);
        List<ActivityEmployeeEntity> entries = raw.stream()
                .filter(ae -> {
                    WorkSchedule ws = ae.getWorkSchedule();
                    return ws == null || ws.getSubstituteEmployee() != null
                            || !("UW".equalsIgnoreCase(ws.getWorkMode()) || "ZL".equalsIgnoreCase(ws.getWorkMode()));
                })
                .collect(Collectors.toList());

        // grupuj po pracowniku
        Map<Integer, List<ActivityEmployeeEntity>> byEmp = entries.stream()
                .collect(Collectors.groupingBy(ae -> ae.getEmployee().getId()));

        // oblicz i zapisz
        Map<Integer, Double> result = new HashMap<>();
        byEmp.forEach((empId, list) -> {
            double score = computeScore(list, entries);
            log.info("Period key={} empId={} → score {}", periodKey, empId, score);
            result.put(empId, score);
            saver.accept(entityFactory.apply(empId, score));
        });

        return result;
    }

    /**
     * Numerator/denominator według zasad F/B, S, U oraz jednorazowe zliczanie work_schedule.
     */
    private double computeScore(List<ActivityEmployeeEntity> mine, List<ActivityEmployeeEntity> all) {
        double num = 0, den = 0;
        Set<Integer> seenWS = new HashSet<>();

        for (ActivityEmployeeEntity ae : mine) {
            ProcedureEntity proc = ae.getActivity().getProcedure();
            String uwagi = proc.getWorkMode();
            Integer pts = proc.getProcedureActualTime();
            long total = all.stream()
                    .filter(x -> x.getActivity().getActivityId().equals(ae.getActivity().getActivityId()))
                    .count();

            // numerator
            if ("F".equalsIgnoreCase(uwagi) || "B".equalsIgnoreCase(uwagi)) {
                if (pts != null && total > 0) num += (double) pts / total;
            } else if ("S".equalsIgnoreCase(uwagi)) {
                if (pts != null) num += pts;
            } else if ("U".equalsIgnoreCase(uwagi)) {
                num += 1;
            }

            // denominator — raz na każdy WS
            WorkSchedule ws = ae.getWorkSchedule();
            if (ws != null && ws.getId() != null && seenWS.add(ws.getId())) {
                Integer dur = ws.getWorkDurationMinutes();
                if (dur != null) den += dur;
            }
        }

        return den>0 ? num/den : 0.0;
    }
}
