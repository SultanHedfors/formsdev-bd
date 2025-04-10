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
    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private volatile boolean processLock = false;

    @Transactional
    @Scheduled(fixedDelayString = "${scheduler.assign-activities.delay:10000}")
    public void scheduledAssignActivitiesToSchedules() {
        if (processLock) {
            log.debug("Process lock active, skipping scheduled execution.");
            return;
        }
        assignActivitiesToSchedules(false);
    }

    @Transactional
    public void assignActivitiesToSchedules(boolean fromScheduleImport) {
        try {
            if (fromScheduleImport) {
                processLock = true;
            }

            List<WorkSchedule> schedules = scheduleRepository.findByActivityIsNull();

            if (schedules.isEmpty()) {
                log.debug("No schedules with null activity found.");
                return;
            }

            log.info("Found {} schedules with null activity", schedules.size());

            Set<Integer> assignedActivityIds = new HashSet<>();

            for (WorkSchedule ws : schedules) {
                Optional<ActivityEntity> optionalActivity = findMatchingActivity(ws);
                if (optionalActivity.isPresent()) {
                    ActivityEntity activity = optionalActivity.get();

                    if (!isWorkModeMatching(ws, activity.getProcedure())) {
                        continue;
                    }

                    ws.setActivity(activity);
                    scheduleRepository.save(ws);
                    assignedActivityIds.add(activity.getActivityId());
                    log.info("Assigned activity {} to schedule {}", activity.getActivityId(), ws.getId());
                }
            }

            if (!assignedActivityIds.isEmpty()) {
                if (fromScheduleImport) {
                    log.info("Cleaning ActivityEmployeeEntity for activities: {}", assignedActivityIds);
                    activityEmployeeRepository.deleteByActivityActivityIdInAndUserModifiedFalse(assignedActivityIds);
                    scheduleRepository.flush();
                }

                log.info("Creating new ActivityEmployeeEntity entries...");
                createNewActivityEmployeeEntries(assignedActivityIds);
            }

        } catch (Exception ex) {
            log.error("Error while assigning activities to schedules", ex);
            throw ex;
        } finally {
            if (fromScheduleImport) {
                processLock = false;
            }
        }
    }

    private Optional<ActivityEntity> findMatchingActivity(WorkSchedule ws) {
        try {
            LocalDate date = LocalDate.of(Integer.parseInt(ws.getYearMonth().substring(0, 4)),
                    Integer.parseInt(ws.getYearMonth().substring(5, 7)),
                    ws.getDayOfMonth());

            LocalTime time;
            try {
                String timeStr = ws.getWorkStartTime().length() == 5 ? ws.getWorkStartTime() + ":00" : ws.getWorkStartTime();
                time = LocalTime.parse(timeStr, TIME_FORMATTER);
            } catch (Exception e) {
                log.warn("Invalid time format in schedule {}: {}", ws.getId(), ws.getWorkStartTime());
                return Optional.empty();
            }

            return activityRepository.findFirstActivityWithoutScheduleByDateTime(
                    date.getYear(),
                    date.getMonthValue(),
                    date.getDayOfMonth(),
                    time.getHour(),
                    time.getMinute(),
                    time.getSecond()
            );
        } catch (Exception ex) {
            log.warn("Failed to resolve date/time for schedule {}", ws.getId(), ex);
            return Optional.empty();
        }
    }

    private void createNewActivityEmployeeEntries(Set<Integer> activityIds) {
        List<ActivityEntity> activities = activityRepository.findAllById(activityIds);

        Set<Integer> skipIds = activityEmployeeRepository
                .findByActivityActivityIdInAndUserModifiedTrue(activityIds)
                .stream()
                .map(ae -> ae.getActivity().getActivityId())
                .collect(Collectors.toSet());

        List<ActivityEmployeeEntity> toCreate = new ArrayList<>();

        for (ActivityEntity activity : activities) {
            if (skipIds.contains(activity.getActivityId())) {
                continue;
            }

            List<WorkSchedule> related = scheduleRepository.findByActivityWithEmployee(activity);
            Set<UserEntity> employees = related.stream()
                    .map(WorkSchedule::getEmployee)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (UserEntity emp : employees) {
                ActivityEmployeeEntity ae = new ActivityEmployeeEntity();
                ae.setActivity(activity);
                ae.setEmployee(emp);
                ae.setUserModified(false);
                toCreate.add(ae);
            }
        }

        if (!toCreate.isEmpty()) {
            activityEmployeeRepository.saveAll(toCreate);
            log.info("Created {} new employee assignments", toCreate.size());
        }
    }

    private boolean isWorkModeMatching(WorkSchedule ws, ProcedureEntity procedure) {
        String scheduleMode = ws.getWorkMode();
        String procMode = Optional.ofNullable(procedure).map(ProcedureEntity::getWorkMode).orElse(null);
        return scheduleMode == null || scheduleMode.equals(procMode);
    }
}
