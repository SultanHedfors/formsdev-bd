package com.example.demo.service;

import com.example.demo.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledActivityToWSService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduledActivityToWSServiceHelper helper;
    private final ScheduleAssignmentJobQueue jobQueue;

    @Scheduled(fixedDelayString = "${scheduler.assign-activities.delay:10000}")
    public void scheduledAssignActivitiesToSchedules() {
        log.info(">>> scheduledAssignActivitiesToSchedules() called – submitting to queue...");

        jobQueue.submitJob(() -> {
            try {
                if (!unprocessedSchedulesExist()) {
                    log.info("No unprocessed schedules found. Skipping assignment process..");
                    return;
                }
                helper.createActivityEmployeeAssignments(false, null);
            } catch (Exception e) {

                log.error("Error running scheduled assignment.", e);


            }
        });
    }


    private boolean unprocessedSchedulesExist() {
        return scheduleRepository.existsUnprocessedSchedules();
    }
}

