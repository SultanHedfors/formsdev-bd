package com.example.demo.service;

import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.ProcedureEntity;
import com.example.demo.entity.WorkSchedule;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledActivityToWSService {

    private final ActivityRepository activityRepository;
    private final ScheduleRepository scheduleRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional
    @Scheduled(fixedDelayString = "${scheduler.assign-activities.delay:10000}")
    public void assignActivitiesToSchedules() {
        try {
            String earliestScheduleDateStr = scheduleRepository.findEarliestScheduleDate();
            if (earliestScheduleDateStr == null) {
                log.debug("No schedules found. Skipping activity assignment.");
                return;
            }

            LocalDate earliestScheduleDate = LocalDate.parse(earliestScheduleDateStr);
            List<ActivityEntity> activities = activityRepository.findActivitiesWithoutScheduleFromDate(earliestScheduleDate.atStartOfDay());

            if (activities.isEmpty()) {
                log.debug("No unassigned activities found from date {}.", earliestScheduleDate);
                return;
            }

            Set<String> activityDates = activities.stream()
                    .map(a -> a.getActivityDate().toLocalDateTime().toLocalDate().toString())
                    .collect(Collectors.toSet());

            List<WorkSchedule> schedules = scheduleRepository.findByDateIn(activityDates);

            if (schedules.isEmpty()) {
                log.debug("No schedules found for activity dates: {}", activityDates);
                return;
            }

            log.info("Processing {} activities against {} schedules.", activities.size(), schedules.size());

            activities.forEach(activity -> processActivity(activity, schedules));

        } catch (Exception ex) {
            log.error("Error while assigning activities to schedules: ", ex);
        }
    }

    private void processActivity(ActivityEntity activity, List<WorkSchedule> schedules) {
        LocalDate activityDate = activity.getActivityDate().toLocalDateTime().toLocalDate();
        LocalTime activityLocalTime = activity.getActivityTime().toLocalDateTime().toLocalTime();

        List<WorkSchedule> matchingSchedules = schedules.stream()
                .filter(ws -> isScheduleMatchingActivity(ws, activityDate, activityLocalTime))
                .toList();

        if (matchingSchedules.isEmpty()) {
            log.debug("No matching schedules found for activity {} (date: {}, time: {}).",
                    activity.getActivityId(), activityDate, activityLocalTime);
            return;
        }

        for (WorkSchedule ws : matchingSchedules) {
            if (ws.getActivity() != null) {
                log.debug("Schedule {} already has assigned activity, skipping.", ws.getId());
                continue;
            }

            if (!isWorkModeMatching(ws, activity.getProcedure())) {
                log.debug("Work mode does not match for schedule {} and activity {}.", ws.getId(), activity.getActivityId());
                continue;
            }

            ws.setActivity(activity);
            scheduleRepository.save(ws);
            log.info("Assigned activity {} to schedule {}", activity.getActivityId(), ws.getId());
        }
    }

    private boolean isScheduleMatchingActivity(WorkSchedule ws, LocalDate activityDate, LocalTime activityLocalTime) {
        try {
            String wsTime = ws.getWorkStartTime();
            if (wsTime.length() == 5) {
                wsTime = wsTime + ":00";
            }
            LocalTime scheduleTime = LocalTime.parse(wsTime, TIME_FORMATTER);

            return ws.getYearMonth().equals(activityDate.format(YM_FORMATTER))
                    && ws.getDayOfMonth() == activityDate.getDayOfMonth()
                    && scheduleTime.equals(activityLocalTime);

        } catch (Exception e) {
            log.warn("Failed to parse schedule time for WorkSchedule ID {}: {}", ws.getId(), e.getMessage());
            return false;
        }
    }

    private boolean isWorkModeMatching(WorkSchedule ws, ProcedureEntity procedure) {
        String scheduleWorkMode = ws.getWorkMode();
        String procedureWorkMode = Optional.ofNullable(procedure)
                .map(ProcedureEntity::getWorkMode)
                .orElse(null);

        return scheduleWorkMode == null || scheduleWorkMode.equals(procedureWorkMode);
    }
}
