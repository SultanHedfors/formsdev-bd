package com.example.demo.schedule.processor;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.demo.util.TimeUtil.addOneSecondToTimeString;
@Component
public class LateWorkSchedulesHandler {

     void addLateWorkSchedulesForLatestEndTime(List<WorkSchedule> workSchedules, YearMonth currentYearMonth) {
        Set<String> workModesToCheck = Set.of("F", "B");

        for (var workMode : workModesToCheck) {
            var byDate = workSchedules.stream()
                    .filter(ws -> workMode.equals(ws.getWorkMode()))
                    .filter(ws -> YearMonth.parse(ws.getYearMonth()).equals(currentYearMonth))
                    .collect(Collectors.groupingBy(ws -> LocalDate.of(
                            currentYearMonth.getYear(),
                            currentYearMonth.getMonthValue(),
                            ws.getDayOfMonth()
                    )));

            for (var entry : byDate.entrySet()) {
                processLateSchedulesForDate(workSchedules, currentYearMonth, workMode, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void processLateSchedulesForDate(
            List<WorkSchedule> workSchedules,
            YearMonth currentYearMonth,
            String workMode,
            LocalDate date,
            List<WorkSchedule> schedulesForDayAndMode
    ) {
        var latestEndTime = schedulesForDayAndMode.stream()
                .map(WorkSchedule::getWorkEndTime)
                .max(String::compareTo)
                .orElse(null);

        if (latestEndTime == null) return;
        var lateStartTime = addOneSecondToTimeString(latestEndTime);

        var employeesWithSchedule = schedulesForDayAndMode.stream()
                .map(ws -> ws.getSubstituteEmployee() != null ? ws.getSubstituteEmployee() : ws.getEmployee())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (var employee : employeesWithSchedule) {
            if (!lateScheduleAlreadyExists(workSchedules, currentYearMonth, workMode, date, lateStartTime, employee)) {
                workSchedules.add(
                        WorkSchedule.builder()
                                .yearMonth(currentYearMonth.toString())
                                .dayOfMonth(date.getDayOfMonth())
                                .employee(employee)
                                .workMode(workMode)
                                .workStartTime(lateStartTime)
                                .workEndTime("23:59:59")
                                .workDurationMinutes(0)
                                .roomSymbol(null)
                                .processed(false)
                                .build()
                );
            }
        }
    }

    private static boolean lateScheduleAlreadyExists(
            List<WorkSchedule> workSchedules,
            YearMonth yearMonth,
            String workMode,
            LocalDate date,
            String lateStartTime,
            UserEntity employee
    ) {
        return workSchedules.stream().anyMatch(ws ->
                workMode.equals(ws.getWorkMode()) &&
                        (ws.getSubstituteEmployee() != null ? ws.getSubstituteEmployee() : ws.getEmployee()).equals(employee) &&
                        YearMonth.parse(ws.getYearMonth()).equals(yearMonth) &&
                        LocalDate.of(
                                yearMonth.getYear(),
                                yearMonth.getMonthValue(),
                                ws.getDayOfMonth()
                        ).equals(date) &&
                        ws.getWorkStartTime().equals(lateStartTime) &&
                        ws.getWorkEndTime().equals("23:59:59")
        );
    }
}
