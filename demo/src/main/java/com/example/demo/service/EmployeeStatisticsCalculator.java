package com.example.demo.service;

import com.example.demo.entity.ActivityEmployeeEntity;
import com.example.demo.entity.ProcedureEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeStatisticsCalculator {

    private final ActivityEmployeeRepository activityEmployeeRepository;
    private final DailyEmployeeStatisticRepository employeeDailyStatsRepository;

    public Map<Integer, Double> calculateDailyScores(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        log.info("Calculating scores for date: {}", date);

        List<ActivityEmployeeEntity> allEntriesRaw = activityEmployeeRepository.findWithGraphByActivityDate(startOfDay, endOfDay);

        List<ActivityEmployeeEntity> allEntries = allEntriesRaw.stream()
                .filter(ae -> {
                    WorkSchedule ws = ae.getWorkSchedule();
                    return ws == null || !"UW".equalsIgnoreCase(ws.getWorkMode()) && !"ZL".equalsIgnoreCase(ws.getWorkMode()) || ws.getSubstituteEmployee() != null;
                })
                .toList();

        log.info("Found {} relevant activity_employee entries", allEntries.size());

        Map<Integer, List<ActivityEmployeeEntity>> groupedByEmployee = allEntries.stream()
                .collect(Collectors.groupingBy(ae -> ae.getEmployee().getId()));

        Map<Integer, Double> result = new HashMap<>();

        for (Map.Entry<Integer, List<ActivityEmployeeEntity>> entry : groupedByEmployee.entrySet()) {
            Integer employeeId = entry.getKey();
            List<ActivityEmployeeEntity> entries = entry.getValue();

            double numerator = 0;
            double denominator = 0;

            log.info("Processing employeeId: {}", employeeId);

            Set<Integer> countedWorkScheduleIds = new HashSet<>();

            for (ActivityEmployeeEntity ae : entries) {
                ProcedureEntity procedure = ae.getActivity().getProcedure();
                String uwagi = procedure.getWorkMode();
                Integer punkty = procedure.getProcedureActualTime();
                int totalAssignments = (int) allEntries.stream()
                        .filter(e -> e.getActivity().getActivityId().equals(ae.getActivity().getActivityId()))
                        .count();

                WorkSchedule ws = ae.getWorkSchedule();
                Integer duration = ws != null ? ws.getWorkDurationMinutes() : null;
                Integer wsId = ws != null ? ws.getId() : null;

                boolean isDebugTarget = wsId != null && (Objects.equals(wsId, 32599) || Objects.equals(wsId, 32110));

                if (isDebugTarget) {
                    log.info("\n[DEBUG TARGET] AE id: {}, activityId: {}, ws.id: {}, uwagi: {}, punkty: {}, totalAssignments: {}, duration: {}",
                            ae.getId(),
                            ae.getActivity().getActivityId(),
                            wsId,
                            uwagi,
                            punkty,
                            totalAssignments,
                            duration
                    );
                }

                if ("F".equalsIgnoreCase(uwagi) || "B".equalsIgnoreCase(uwagi)) {
                    if (punkty != null && totalAssignments > 0) {
                        double value = (double) punkty / totalAssignments;
                        numerator += value;
                        if (isDebugTarget) {
                            log.info("[DEBUG TARGET] Add to numerator (F/B): {} / {} = {}", punkty, totalAssignments, value);
                        }
                    }
                } else if ("S".equalsIgnoreCase(uwagi)) {
                    if (punkty != null) {
                        numerator += punkty;
                        if (isDebugTarget) {
                            log.info("[DEBUG TARGET] Add to numerator (S): {}", punkty);
                        }
                    }
                } else if ("U".equalsIgnoreCase(uwagi)) {
                    numerator += 1.0;
                    if (isDebugTarget) {
                        log.info("[DEBUG TARGET] Add to numerator (U): 1.0");
                    }
                }

                if (duration != null && wsId != null && !countedWorkScheduleIds.contains(wsId)) {
                    denominator += duration;
                    countedWorkScheduleIds.add(wsId);
                    if (isDebugTarget) {
                        log.info("[DEBUG TARGET] Add to denominator: {} (ws.id: {})", duration, wsId);
                    }
                }
            }

            double score = denominator > 0 ? numerator / denominator : 0.0;
            result.put(employeeId, score);
            log.info("Calculated score for employeeId {}: {} / {} = {}", employeeId, numerator, denominator, score);

            EmployeeDailyStatsEntity stats = new EmployeeDailyStatsEntity(
                    employeeId,
                    date,
                    score,
                    LocalDateTime.now()
            );
            employeeDailyStatsRepository.save(stats);
            log.info("Saved EmployeeDailyStatsEntity: {}", stats);
        }

        return result;
    }
}
