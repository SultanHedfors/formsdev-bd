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
import java.time.YearMonth;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledActivityToWSServiceHelper {

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
                log.info("running from ScheduleReader");
                deleteOldAssignmentsForPeriod(yearMonth);
            } else {
                log.info("running from scheduled job");
            }
            final Timestamp createdAt = new Timestamp(System.currentTimeMillis());
            log.info("starting insert...");
            runInsertAssignments(INSERT_NEW_ACTIVITY_EMPLOYEE_SQL_PATH, createdAt);
            log.info("Assignments created, now updating processed flag..");
            runInsertAssignments(SET_ASSIGNED_SCHEDULES_PROCESSED_SQL_PATH, createdAt);
            entityManager.clear();

            if (fromScheduleImport) {
                log.info("Setting processed for rows with current schedule's period");
                scheduleRepository.setProcessedByYearMonth(yearMonth);
            }
            log.info("New ActivityEmployee assignments have been created.");
    }



    /*
        Inserts new assignments into activity_employee for all unprocessed work schedules.

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
        - After insert sets processed flag=1 for affected schedules
    */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public void runInsertAssignments(String resourcePath, Timestamp createdAt) throws IOException {
        log.info("running query {}", resourcePath);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            String sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            jdbcTemplate.update(sql, createdAt);
            log.info("Query done");
        }
    }


    private void deleteOldAssignmentsForPeriod(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        Timestamp from = Timestamp.valueOf(ym.atDay(1).atStartOfDay());
        Timestamp to = Timestamp.valueOf(ym.atEndOfMonth().atTime(23, 59, 59));

        int deleted = activityEmployeeRepository.deleteAllByActivityDateRangeAndUserModifiedFalse(from, to);
        log.info("Deleted {} non-user-modified activity_employee entries from {}", deleted, yearMonth);
    }
}
