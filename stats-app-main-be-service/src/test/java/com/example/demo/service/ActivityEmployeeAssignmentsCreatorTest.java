package com.example.demo.service;

import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.ScheduleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActivityEmployeeAssignmentsCreatorTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock ActivityEmployeeRepository activityEmployeeRepository;
    @Mock ScheduleRepository scheduleRepository;
    @Mock EntityManager entityManager;

    ActivityEmployeeAssignmentsCreator creator;

    // Trzymamy wszystkie streamy do zamknięcia po testach
    private final List<ByteArrayInputStream> openedStreams = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creator = spy(new ActivityEmployeeAssignmentsCreator(jdbcTemplate, activityEmployeeRepository, scheduleRepository));
        TestUtils.setField(creator, entityManager);
        openedStreams.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        for (var stream : openedStreams) {
            stream.close();
        }
        openedStreams.clear();
    }

    @Test
    void createActivityEmployeeAssignments_fromScheduleImport_true_callsDeleteAndSetProcessed() throws Exception {
        // arrange
        String yearMonth = "2024-07";
        mockResource(ActivityEmployeeAssignmentsCreator.INSERT_NEW_ACTIVITY_EMPLOYEE_SQL_PATH, "INSERT1");
        mockResource(ActivityEmployeeAssignmentsCreator.SET_ASSIGNED_SCHEDULES_PROCESSED_SQL_PATH, "UPDATE1");

        when(activityEmployeeRepository.deleteAllByActivityDateRangeAndUserModifiedFalse(any(), any())).thenReturn(5);

        // act
        creator.createActivityEmployeeAssignments(true, yearMonth);

        // assert
        verify(activityEmployeeRepository).deleteAllByActivityDateRangeAndUserModifiedFalse(any(), any());
        verify(scheduleRepository).setProcessedByYearMonth(eq(yearMonth));
        verify(jdbcTemplate, times(2)).update(anyString(), any(Timestamp.class));
        verify(entityManager).clear();
    }

    @Test
    void createActivityEmployeeAssignments_fromScheduleImport_false_doesNotCallDeleteOrSetProcessed() throws Exception {

        // arrange
        mockResource(ActivityEmployeeAssignmentsCreator.INSERT_NEW_ACTIVITY_EMPLOYEE_SQL_PATH, "INSERT2");
        mockResource(ActivityEmployeeAssignmentsCreator.SET_ASSIGNED_SCHEDULES_PROCESSED_SQL_PATH, "UPDATE2");

        // act
        creator.createActivityEmployeeAssignments(false, null);

        // assert
        verify(activityEmployeeRepository, never()).deleteAllByActivityDateRangeAndUserModifiedFalse(any(), any());
        verify(scheduleRepository, never()).setProcessedByYearMonth(any());
        verify(jdbcTemplate, times(2)).update(anyString(), any(Timestamp.class));
        verify(entityManager).clear();
    }

    @Test
    void runInsertAssignments_resourceNotFound_throwsFileNotFoundException() {
        assertThrows(FileNotFoundException.class,
                () -> creator.runInsertAssignments("not-found.sql", "Query", new Timestamp(System.currentTimeMillis())));
    }

    @Test
    void deleteOldAssignmentsForPeriod_executesDeleteWithCorrectParams() {

        // arrange
        String yearMonth = "2024-07";
        when(activityEmployeeRepository.deleteAllByActivityDateRangeAndUserModifiedFalse(any(), any()))
                .thenReturn(123);

        // act
        creator.deleteOldAssignmentsForPeriod(yearMonth);

        // assert
        verify(activityEmployeeRepository).deleteAllByActivityDateRangeAndUserModifiedFalse(any(), any());
    }

    @SuppressWarnings("resource")
    private void mockResource(String resourcePath, String sqlContent) {
        ByteArrayInputStream stream = new ByteArrayInputStream(sqlContent.getBytes());
        openedStreams.add(stream);
        doReturn(stream)
                .when(creator).getResourceAsStream(eq(resourcePath));
    }

    static class TestUtils {
        static void setField(Object target, Object value) {
            try {
                var f = target.getClass().getDeclaredField("entityManager");
                f.setAccessible(true);
                f.set(target, value);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Object " + target.getClass() + " does not have field entityManager", e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
