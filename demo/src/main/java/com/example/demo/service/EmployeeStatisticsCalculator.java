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

        BiFunction<Integer, Double, EmployeeDailyStatsEntity> dailyStatsEntityCreator =
                (empId, score) ->
                        new EmployeeDailyStatsEntity(empId, date, score, LocalDateTime.now());

        return calculateAndSave(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                date,
                dailyStatsEntityCreator,
                dailyRepo::save
        );
    }

    public void calculateWeeklyScores(LocalDate monday) {
        var start = monday.atStartOfDay();
        var end = start.plusWeeks(1);

        BiFunction<Integer, Double, EmployeeWeeklyStatsEntity> weeklyStatsEntityCreator =
                (empId, score) ->
                        new EmployeeWeeklyStatsEntity(empId, monday, score, LocalDateTime.now());

        calculateAndSave(
                start,
                end,
                monday,
                weeklyStatsEntityCreator,
                weeklyRepo::save
        );
    }

    public void calculateMonthlyScores(YearMonth month) {
        var start = month.atDay(1).atStartOfDay();
        var end = month.plusMonths(1).atDay(1).atStartOfDay();

        BiFunction<Integer, Double, EmployeeMonthlyStatsEntity> monthlyStatsEntityCreator =
                (empId, score) ->
                        new EmployeeMonthlyStatsEntity(empId, month.toString(), score, LocalDateTime.now());

        calculateAndSave(
                start,
                end,
                month.toString(),
                monthlyStatsEntityCreator,
                monthlyRepo::save
        );
    }

    public void calculateYearlyScores(Year year) {
        var start = year.atDay(1).atStartOfDay();
        var end = start.plusYears(1);

        BiFunction<Integer, Double, EmployeeYearlyStatsEntity> yearlyStatsEntityCreator =
                (empId, score) ->
                        new EmployeeYearlyStatsEntity(empId, year.getValue(), score, LocalDateTime.now());

        calculateAndSave(
                start,
                end,
                year.getValue(),
                yearlyStatsEntityCreator,
                yearlyRepo::save
        );
    }

    private <T> Map<Integer, Double> calculateAndSave(
            LocalDateTime start,
            LocalDateTime end,
            Object periodKey,
            BiFunction<Integer, Double, T> entityCreator,
            java.util.function.Consumer<T> saver
    ) {
        log.debug("Calculating stats for period starting {} to {} (key={})", start, end, periodKey);

        List<ActivityEmployeeEntity> allEmployeesActivities = getEntitiesBySelectedPeriod(start, end);

        var entriesGroupedByEmployee = allEmployeesActivities.stream()
                .collect(Collectors
                        .groupingBy(ae -> ae.getEmployee().getId()));

        // calculate & save
        var result = new HashMap<Integer, Double>();
        entriesGroupedByEmployee
                .forEach((empId, activitiesForEmployee) -> {
                    double score = computeScore(activitiesForEmployee, allEmployeesActivities);
                    log.debug("Period key={} empId={} → score {}", periodKey, empId, score);
                    result.put(empId, score);
                    saver.accept(entityCreator.apply(empId, score));
                });

        return result;
    }

    private List<ActivityEmployeeEntity> getEntitiesBySelectedPeriod(LocalDateTime start, LocalDateTime end) {
        List<ActivityEmployeeEntity> entitiesInSelectedPeriod = activityRepo.findWithGraphByActivityDate(start, end);

        //filter leaving in the list only assignments with no schedule with no absence
        return entitiesInSelectedPeriod
                .stream()
                .filter(ae -> {
                    WorkSchedule ws = ae.getWorkSchedule();
                    return ws == null
                            || ws.getSubstituteEmployee() != null
                            || Optional.ofNullable(ws.getWorkMode())
                            .map(String::toUpperCase)
                            .map(mode -> !Set.of("UW", "ZL").contains(mode))
                            .orElse(true);
                }).toList();
    }

    private double computeScore(List<ActivityEmployeeEntity> currentEmployeesActivities,
                                List<ActivityEmployeeEntity> allActivities) {
        double numerator = currentEmployeesActivities.stream()
                .mapToDouble(currEmployeeActivity ->
                        computeNumeratorFor(currEmployeeActivity, allActivities))
                .sum();

        double denominator = currentEmployeesActivities.stream()
                .map(ActivityEmployeeEntity::getWorkSchedule)
                .filter(Objects::nonNull)
                .filter(ws -> ws.getId() != null)
                .distinct()
                .mapToDouble(ws -> Optional.ofNullable(ws.getWorkDurationMinutes())
                        .orElse(0))
                .sum();

        return denominator > 0 ? numerator / denominator : 0.0;
    }

    private double computeNumeratorFor(ActivityEmployeeEntity currentEmployeesActivity,
                                       List<ActivityEmployeeEntity> allActivityEmployees) {
        ProcedureEntity proc = currentEmployeesActivity.getActivity().getProcedure();
        String workMode = proc.getWorkMode();
        Integer procedureMinutes = proc.getProcedureActualTime();

        long numEmployeesAssignedToActivity = allActivityEmployees.stream()
                .filter(act ->
                        act.getActivity().getActivityId()
                                .equals(currentEmployeesActivity.getActivity().getActivityId()))
                .count();

        if ("F".equalsIgnoreCase(workMode) || "B".equalsIgnoreCase(workMode)) {
            return (procedureMinutes != null && numEmployeesAssignedToActivity > 0) ?
                    (double) procedureMinutes / numEmployeesAssignedToActivity : 0.0;
        } else if ("S".equalsIgnoreCase(workMode)) {
            return procedureMinutes != null ? procedureMinutes : 0.0;
        } else if ("U".equalsIgnoreCase(workMode)) {
            return 1.0;
        }
        return 0.0;
    }
}
