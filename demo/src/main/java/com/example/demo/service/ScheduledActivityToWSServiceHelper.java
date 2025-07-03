package com.example.demo.service;


import com.example.demo.repository.ActivityEmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    //flag preventing concurrent processing
    @Getter
    private volatile boolean processLock = false;

    @Transactional
    void assignActivitiesToSchedules(boolean fromScheduleImport, String yearMonth) throws IOException {
        log.info(">>> Assigning activities to unprocessed schedules");

        if (processLock) {
            log.info(">>> Process already running, skipping execution.");
            return;
        }
        processLock = true;
        try {
            if (fromScheduleImport) {
                deleteOldRecordsForPeriod(yearMonth);
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
*/
            runSqlScript(Path.of(INSERT_NEW_ACTIVITY_EMPLOYEE_SQL_PATH));

//Sets 'PROCESSED' flag for all schedules affected by insert above
            runSqlScript(Path.of(SET_ASSIGNED_SCHEDULES_PROCESSED_SQL_PATH));
            entityManager.clear();
        } finally {
            processLock = false;
            log.debug("Schedules processing unlocked");
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public void runSqlScript(Path sqlPath) throws IOException {
        String sql;
        try (var stream = Files.lines(sqlPath)) {
            sql = stream.reduce("", (scriptLine, newLine) -> scriptLine + "\n" + newLine);
        }
        jdbcTemplate.update(sql);
    }


    private void deleteOldRecordsForPeriod(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        Timestamp from = Timestamp.valueOf(ym.atDay(1).atStartOfDay());
        Timestamp to = Timestamp.valueOf(ym.atEndOfMonth().atTime(23, 59, 59));

        int deleted = activityEmployeeRepository.deleteAllByActivityDateRangeAndUserModifiedFalse(from, to);
        log.info("Deleted {} non-user-modified activity_employee entries from {}", deleted, yearMonth);
    }
}
