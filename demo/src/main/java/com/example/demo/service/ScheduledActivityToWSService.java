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
import java.time.YearMonth;
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
            assignActivitiesToSchedules(false,null);
        } catch (Exception ex) {
            log.error("Scheduled task failed", ex);
        }
    }

    @Transactional
    public void assignActivitiesToSchedules(boolean fromScheduleImport, String yearMonth) {
        log.info(">>> Assigning activities to unprocessed schedules");

        if (fromScheduleImport) {
            processLock=true;
            YearMonth ym = YearMonth.parse(yearMonth);
            Timestamp from = Timestamp.valueOf(ym.atDay(1).atStartOfDay());
            Timestamp to = Timestamp.valueOf(ym.atEndOfMonth().atTime(23, 59, 59));

            int deleted = activityEmployeeRepository.deleteAllByActivityDateRangeAndUserModifiedFalse(from, to);
            log.info("Deleted {} non-user-modified activity_employee entries from {}", deleted, yearMonth);
        }

        List<Integer> scheduleIds = scheduleRepository.findIdsOfUnprocessedSchedules();
        log.info("Found {} unprocessed schedules", scheduleIds.size());
        if (scheduleIds.isEmpty()) {
            log.info("No unprocessed schedules found.");
            return;
        }

        List<WorkSchedule> schedules = scheduleRepository.findAllById(scheduleIds);
        log.info("Loaded {} work schedules by ID", schedules.size());
        if (schedules.isEmpty()) {
            log.info("No schedules loaded.");
            return;
        }

        int totalAssigned = 0;
        for (WorkSchedule ws : schedules) {
            try {
                log.debug("Processing schedule ID={} | yearMonth={} | day={}", ws.getId(), ws.getYearMonth(), ws.getDayOfMonth());

                LocalDate date = LocalDate.of(
                        Integer.parseInt(ws.getYearMonth().substring(0, 4)),
                        Integer.parseInt(ws.getYearMonth().substring(5, 7)),
                        ws.getDayOfMonth()
                );

                LocalTime startTimeRaw = parseTime(ws.getWorkStartTime());
                LocalTime endTimeRaw = parseTime(ws.getWorkEndTime());
                if (startTimeRaw == null || endTimeRaw == null) {
                    log.warn("Invalid time for schedule ID={}: {} - {}", ws.getId(), ws.getWorkStartTime(), ws.getWorkEndTime());
                    continue;
                }

                Timestamp startOfDay = Timestamp.valueOf(date.atStartOfDay());
                Timestamp endOfDay = Timestamp.valueOf(date.plusDays(1).atStartOfDay().minusSeconds(1));

                log.debug("Querying activities for WS ID={} between {} and {}", ws.getId(), startOfDay, endOfDay);
                List<ActivityEntity> activities = activityRepository.findActivitiesInDateRangeWithProcedure(startOfDay, endOfDay);
                log.debug("Retrieved {} activities for WS ID={}", activities.size(), ws.getId());
                if (activities.isEmpty()) continue;

                List<Integer> activityIds = activities.stream()
                        .map(ActivityEntity::getActivityId)
                        .toList();

                Map<Integer, Boolean> manualModifiedMap = activityEmployeeRepository
                        .findManualModifiedActivityIds(activityIds)
                        .stream().collect(Collectors.toMap(id -> id, id -> true));
                log.debug("Manual-modified activities map created for WS ID={}", ws.getId());

                Set<String> existingAssignments = activityEmployeeRepository.findAllExistingActivityEmployeePairs();
                log.debug("Existing activity-employee pairs fetched");

                List<ActivityEmployeeEntity> toSave = new ArrayList<>();

                for (ActivityEntity act : activities) {
                    if (act.getActivityTime() == null) continue;

                    LocalTime time = act.getActivityTime().toLocalDateTime().toLocalTime();
                    if (time.isBefore(startTimeRaw) || time.isAfter(endTimeRaw) ) continue;

                    if (!ws.getWorkMode().equals(act.getProcedure().getWorkMode())) continue;

                    if (!"F".equals(ws.getWorkMode()) && !"B".equals(ws.getWorkMode())) {
                        if (act.getRoom() == null || ws.getRoomSymbol() == null ||
                                !ws.getRoomSymbol().equalsIgnoreCase(act.getRoom().getRoomCode())) {
                            continue;
                        }
                    }

                    if (manualModifiedMap.getOrDefault(act.getActivityId(), false)) {
                        log.debug("Skipping activity {} due to user_modified=true", act.getActivityId());
                        continue;
                    }

                    UserEntity emp = ws.getSubstituteEmployee() != null ? ws.getSubstituteEmployee() : ws.getEmployee();
                    if (emp == null) {
                        log.warn("No employee found for WS ID={}", ws.getId());
                        continue;
                    }

                    String key = act.getActivityId() + ":" + emp.getId();
                    if (existingAssignments.contains(key)) {
                        log.debug("Duplicate assignment skipped: {} -> {}", act.getActivityId(), emp.getId());
                        continue;
                    }

                    ActivityEmployeeEntity ae = new ActivityEmployeeEntity();
                    ae.setActivity(act);
                    ae.setEmployee(emp);
                    ae.setUserModified(false);
                    ae.setWorkSchedule(ws);

                    toSave.add(ae);
                }

                if (!toSave.isEmpty()) {
                    activityEmployeeRepository.saveAll(toSave);
                    log.info("Saved {} new activity-employee assignments for WS ID={}", toSave.size(), ws.getId());
                    totalAssigned += toSave.size();
                    ws.setProcessed(true);
                    scheduleRepository.save(ws);

                } else {
                    log.debug("No new assignments to save for WS ID={}", ws.getId());
                }

                ws.setProcessed(true);
                scheduleRepository.save(ws);
                log.debug("Marked WS ID={} as processed", ws.getId());

            } catch (Exception ex) {
                log.error("Failed to assign WS ID={} due to: {}", ws.getId(), ex.getMessage(), ex);
            }
        }
        processLock=false;
        log.info("Finished processing schedules. Total assignments created: {}", totalAssigned);
    }



    private LocalTime parseTime(String timeStr) {
        try {
            return timeStr.length() == 5 ? LocalTime.parse(timeStr + ":00", TIME_FORMATTER) : LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception ex) {
            return null;
        }
    }


}
