package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.ActivityRepository;
import com.example.demo.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledActivityToWSService {

    private final ActivityRepository activityRepository;
    private final ScheduleRepository scheduleRepository;
    private final ActivityEmployeeRepository activityEmployeeRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private volatile boolean processLock = false;

    @Scheduled(fixedDelayString = "${scheduler.assign-activities.delay:10000}")
    public void scheduledAssignActivitiesToSchedules() {
        log.debug(">>> scheduledAssignActivitiesToSchedules() called – checking processLock...");

        if (processLock) {
            log.debug("Process lock active, skipping scheduled execution.");
            return;
        }

        try {
            assignActivitiesToSchedules(false);
        } catch (Exception ex) {
            log.error("Scheduled task failed", ex);
        }
    }

    @Transactional
    public void assignActivitiesToSchedules(boolean fromScheduleImport) {
        log.info(">>> Assigning activities to unprocessed schedules");

        List<Integer> scheduleIds = scheduleRepository.findIdsOfUnprocessedSchedules();
        if (scheduleIds.isEmpty()) {
            log.info("No unprocessed schedules found.");
            return;
        }

        List<WorkSchedule> schedules = scheduleRepository.findAllById(scheduleIds);
        if (schedules.isEmpty()) {
            log.info("No unprocessed schedules found.");
            return;
        }

        int totalAssigned = 0;
        for (WorkSchedule ws : schedules) {
            try {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(ws.getYearMonth().substring(0, 4)),
                        Integer.parseInt(ws.getYearMonth().substring(5, 7)),
                        ws.getDayOfMonth()
                );

                LocalTime startTime = parseTime(ws.getWorkStartTime());
                LocalTime endTime = parseTime(ws.getWorkEndTime());
                if (startTime == null || endTime == null) {
                    log.warn("Invalid time for schedule ID={}: {} - {}", ws.getId(), ws.getWorkStartTime(), ws.getWorkEndTime());
                    continue;
                }

                log.debug("Processing WS ID={} for date {} time range {} - {}",
                        ws.getId(), date, startTime, endTime);

                Timestamp startOfDay = Timestamp.valueOf(date.atStartOfDay());
                Timestamp endOfDay = Timestamp.valueOf(date.plusDays(1).atStartOfDay().minusSeconds(1));

                List<ActivityEntity> activities = activityRepository.findActivitiesInDateRange(startOfDay, endOfDay);

                List<ActivityEntity> matching;
                if ("F".equals(ws.getWorkMode()) || "B".equals(ws.getWorkMode())) {
                    matching = activities.stream()
                            .filter(a -> isWithinRange(a.getActivityTime(), startTime, endTime))
                            .filter(a -> a.getProcedure() != null && ws.getWorkMode().equals(a.getProcedure().getWorkMode()))
                            .collect(Collectors.toList());
                } else {
                    matching = activities.stream()
                            .filter(a -> isWithinRange(a.getActivityTime(), startTime, endTime))
                            .filter(a -> a.getProcedure() != null
                                    && ws.getWorkMode().equals(a.getProcedure().getWorkMode())
                                    && a.getRoom() != null
                                    && ws.getRoomSymbol() != null
                                    && ws.getRoomSymbol().equalsIgnoreCase(a.getRoom().getRoomCode()))
                            .collect(Collectors.toList());
                }

                log.debug("Found {} matching activities for WS ID={}", matching.size(), ws.getId());

                for (ActivityEntity act : matching) {
                    UserEntity emp = ws.getSubstituteEmployee() != null ? ws.getSubstituteEmployee() : ws.getEmployee();
                    if (emp == null) {
                        log.warn("No employee found for WS ID={}", ws.getId());
                        continue;
                    }

                    // Check if this combination already exists
                    boolean exists = activityEmployeeRepository.existsByActivityAndEmployee(act.getActivityId(), emp.getId());
                    if (exists) {
                        log.debug("Skipping duplicate entry for ACTIVITY ID={} and EMPLOYEE ID={}", act.getActivityId(), emp.getId());
                        continue;
                    }

                    ActivityEmployeeEntity ae = new ActivityEmployeeEntity();
                    ae.setActivity(act);
                    ae.setEmployee(emp);
                    ae.setUserModified(false);

                    activityEmployeeRepository.save(ae);

                    log.info("Created ActivityEmployee link: WS ID={} -> ACTIVITY ID={} -> EMPLOYEE ID={}",
                            ws.getId(), act.getActivityId(), emp.getId());

                    totalAssigned++;
                }

                ws.setProcessed(true);
                scheduleRepository.save(ws);

            } catch (Exception ex) {
                log.error("Failed to assign schedule ID={} due to error: {}", ws.getId(), ex.getMessage(), ex);
            }
        }

        log.info("Finished processing schedules. Total assignments created: {}", totalAssigned);
    }


    private LocalTime parseTime(String timeStr) {
        try {
            return timeStr.length() == 5 ? LocalTime.parse(timeStr + ":00", TIME_FORMATTER) : LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isWithinRange(Timestamp activityTime, LocalTime start, LocalTime end) {
        if (activityTime == null) return false;
        LocalTime activityLocalTime = activityTime.toLocalDateTime().toLocalTime();
        return (activityLocalTime.equals(start) || activityLocalTime.isAfter(start)) &&
                (activityLocalTime.equals(end) || activityLocalTime.isBefore(end));
    }




}
