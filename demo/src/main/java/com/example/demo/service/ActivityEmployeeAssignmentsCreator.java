package com.example.demo.service;


import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.ScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityEmployeeAssignmentsCreator {

    private static final String INSERT_NEW_ACTIVITY_EMPLOYEE_SQL_PATH = "sql/ins_activity_employee_assignments_for_unprocessed_ws.sql";
    private static final String SET_ASSIGNED_SCHEDULES_PROCESSED_SQL_PATH = "sql/set_assigned_schedules_processed.sql";

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    private final ActivityEmployeeRepository activityEmployeeRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void createActivityEmployeeAssignments(boolean fromScheduleImport, String yearMonth) throws IOException {

        log.info(">>> Assigning activities to unprocessed schedules");

        if (fromScheduleImport) {
            log.info("ActivityEmployee assignment triggerred by user's schedule upload.");
            deleteOldAssignmentsForPeriod(yearMonth);
        } else {
            log.info("ActivityEmployee assignment running from scheduled job.");
        }

        final Timestamp createdAt = new Timestamp(System.currentTimeMillis());
        log.info("Starting insert...");
        runInsertAssignments(INSERT_NEW_ACTIVITY_EMPLOYEE_SQL_PATH, "Insert assignments", createdAt);

        log.info("Assignments created, now updating processed flag..");
        runInsertAssignments(SET_ASSIGNED_SCHEDULES_PROCESSED_SQL_PATH, "Update PROCESSED flag", createdAt);
        entityManager.clear();

    /*  Setting PROCESSED=1 is done again if assignment was triggered by user to prevent
        scheduled task from retrying assignments of schedules with no matching activities.

        When new Activity(Zajecie) is added, processed flag for schedules matching by date
        is set back to 0 (via DB trigger), to apply assignments on next scheduled job.*/

        if (fromScheduleImport) {
            log.info("Setting processed for rows with current schedule's period");
            scheduleRepository.setProcessedByYearMonth(yearMonth);
        }
        log.info("New ActivityEmployee assignments have been created.");
    }

    /*    Inserts new assignments into activity_employee for all unprocessed work schedules.

        - Matches each work_schedule to activities based on date and time window.
        - For each assignment, uses substitute_employee_id if present, otherwise employee_id.
        - Skips assignments already manually modified by user (user_modified=1).
        - Matching rules by work_mode:
            • For 'UW' and 'ZL':
                - room_symbol (from work_schedule) must match stanowisko_uwagi (from stanowisko)
                - substitute_employee_id must exist
            • For 'F', 'B', 'U':
                - work_mode (from work_schedule) must match zabieg_uwagi (from zabieg)
            • For 'S':
                - room_symbol (from work_schedule) must match stanowisko_uwagi (from stanowisko)
        - Ensures each work_schedule is only processed if not already marked as processed.
        - AFTER INSERT METHOD IS CALLED AGAIN TO UPDATE PROCESSED FLAG = 1, FOR AFFECTED SCHEDULES  */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public void runInsertAssignments(String resourcePath, String queryType, Timestamp createdAt) throws IOException {
        log.info("running {} query", queryType);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            String sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            jdbcTemplate.update(sql, createdAt);
            log.info("{} Query done", queryType);
        }
    }


    private void deleteOldAssignmentsForPeriod(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);

        int deleted = activityEmployeeRepository.deleteAllByActivityDateRangeAndUserModifiedFalse(from, to);
        log.info("Deleted {} non-user-modified activity_employee entries from {}", deleted, yearMonth);
    }
}
