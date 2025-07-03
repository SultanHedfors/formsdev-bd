package com.example.demo.service;

import com.example.demo.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;


@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledActivityToWSService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduledActivityToWSServiceHelper helper;

    @Scheduled(fixedDelayString = "${scheduler.assign-activities.delay:10000}")
    @Transactional
    @Async
    public void scheduledAssignActivitiesToSchedules() throws IOException {
        log.debug(">>> scheduledAssignActivitiesToSchedules() called – checking processLock...");
        if (helper.isProcessLock()) {
            log.debug("Process lock active, skipping scheduled execution.");
            return;
        }
        if (!unprocessedSchedulesExist()) {
            log.debug("No unprocessed schedules found. Skipping assignment process..");
            return;
        }
        helper.assignActivitiesToSchedules(false, null);
        log.info("New ActivityEmployee assignments have been created.");
    }

    @Async
    @Transactional
    public void assignActivitiesToSchedulesAsync(boolean fromScheduleImport, String yearMonth) throws IOException {
        helper.assignActivitiesToSchedules(fromScheduleImport, yearMonth);
    }

    private boolean unprocessedSchedulesExist() {
        return scheduleRepository.existsUnprocessedSchedules();
    }
}

