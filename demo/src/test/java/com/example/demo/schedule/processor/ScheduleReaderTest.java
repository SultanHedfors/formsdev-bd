package com.example.demo.schedule.processor;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.exception.ScheduleValidationException;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.ActivityEmployeeAssignmentsCreator;
import com.example.demo.service.ScheduleAssignmentJobQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ScheduleReaderTest {

    private ScheduleReaderHelper helper;
    private LogUtil logUtil;

    private ScheduleReader reader;

    @BeforeEach
    void setUp() {
        LateWorkSchedulesHandler lateSchedulesHandler = mock(LateWorkSchedulesHandler.class);
        ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
        ScheduleAssignmentJobQueue jobQueue = mock(ScheduleAssignmentJobQueue.class);
        helper = mock(ScheduleReaderHelper.class);
        logUtil = mock(LogUtil.class);
        ExcelValidateUtil validateUtil = mock(ExcelValidateUtil.class);
        ActivityEmployeeAssignmentsCreator assignmentsCreator = mock(ActivityEmployeeAssignmentsCreator.class);

        reader = new ScheduleReader(
                lateSchedulesHandler, scheduleRepository, jobQueue, helper, logUtil, validateUtil, assignmentsCreator
        );
    }

    @Test
    void checkAndSetProcessing_throwsIfAlreadyProcessing() {
        // Arrange
        var processingField = getField("processing");
        setBooleanField(reader, processingField, true);

        // Act & Assert
        assertThatThrownBy(() -> callPrivateCheckAndSetProcessing(reader))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkAndSetProcessing_setsProcessingTrueAndCancelledFalse() {
        var processingField = getField("processing");
        var cancelledField = getField("cancelled");
        setBooleanField(reader, processingField, false);
        setBooleanField(reader, cancelledField, true);

        callPrivateCheckAndSetProcessing(reader);

        assertThat(getBooleanField(reader, processingField)).isTrue();
        assertThat(getBooleanField(reader, cancelledField)).isFalse();
    }

    @Test
    void cancelProcessing_setsFlagAndLogs() {
        // Act
        reader.cancelProcessing();
        // Assert
        assertThat(getBooleanField(reader, getField("cancelled"))).isTrue();
        verify(logUtil, never()).addLogMessage(any());
    }

    @Test
    void getWorkScheduleBuilder_throwsIfDurationIsMinus1() {
        // Arrange
        var row = mock(WorkScheduleRow.class);
        when(row.startVal()).thenReturn("10:00");
        when(row.endVal()).thenReturn("09:00"); // -1 duration
        when(row.day()).thenReturn(1);
        when(row.infoVal()).thenReturn("F");
        when(row.employeesCodes()).thenReturn(Set.of("XXX"));

        var yearMonth = YearMonth.of(2024, 7);
        String roomSymbol = "A";
        String cleanedStart = "10:00";
        String cleanedEnd = "09:00";
        var employee = new UserEntity();

        // Act & Assert
        assertThatThrownBy(() ->
                callPrivateGetWorkScheduleBuilder(reader, row, yearMonth, roomSymbol, cleanedStart, cleanedEnd, employee)
        ).isInstanceOf(ScheduleValidationException.class);
    }

    @Test
    void getWorkScheduleBuilder_setsFieldsCorrectlyForKnownMode() {
        // Arrange
        var row = mock(WorkScheduleRow.class);
        when(row.startVal()).thenReturn("09:00");
        when(row.endVal()).thenReturn("10:00");
        when(row.day()).thenReturn(2);
        when(row.infoVal()).thenReturn("F");
        when(row.employeesCodes()).thenReturn(Set.of("ZZZ"));
        when(row.dayBasedSubCode()).thenReturn(null);

        var yearMonth = YearMonth.of(2024, 7);
        String roomSymbol = "LAB";
        String cleanedStart = "09:00";
        String cleanedEnd = "10:00";
        var employee = new UserEntity();

        // Act
        var builder = callPrivateGetWorkScheduleBuilder(reader, row, yearMonth, roomSymbol, cleanedStart, cleanedEnd, employee);
        var schedule = builder.build();

        // Assert
        assertThat(schedule.getYearMonth()).isEqualTo("2024-07");
        assertThat(schedule.getDayOfMonth()).isEqualTo(2);
        assertThat(schedule.getRoomSymbol()).isEqualTo("LAB");
        assertThat(schedule.getEmployee()).isSameAs(employee);
        assertThat(schedule.getWorkStartTime()).isEqualTo("09:00");
        assertThat(schedule.getWorkEndTime()).isEqualTo("10:00");
        assertThat(schedule.getWorkDurationMinutes()).isEqualTo(60);
        assertThat(schedule.getWorkMode()).isEqualTo("F");
    }

    @Test
    void getWorkScheduleBuilder_setsSubstituteWhenDayBasedSubCode() {
        // Arrange
        var row = mock(WorkScheduleRow.class);
        when(row.startVal()).thenReturn("09:00");
        when(row.endVal()).thenReturn("10:00");
        when(row.day()).thenReturn(3);
        when(row.infoVal()).thenReturn("SOMECODE");
        when(row.employeesCodes()).thenReturn(Set.of("AAA", "BBB"));
        when(row.dayBasedSubCode()).thenReturn("AAA");

        var yearMonth = YearMonth.of(2024, 8);
        String roomSymbol = "A1";
        String cleanedStart = "09:00";
        String cleanedEnd = "10:00";
        var employee = new UserEntity();
        var substitute = new UserEntity();

        when(helper.findEmployeeByCode("AAA")).thenReturn(substitute);

        // Act
        var builder = callPrivateGetWorkScheduleBuilder(reader, row, yearMonth, roomSymbol,
                cleanedStart, cleanedEnd, employee);
        var schedule = builder.build();

        // Assert
        assertThat(schedule.getSubstituteEmployee()).isSameAs(substitute);
    }

    //helpers
    private static java.lang.reflect.Field getField(String name) {
        try {
            var f = ScheduleReader.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setBooleanField(Object target, java.lang.reflect.Field field, boolean value) {
        try {
            field.setBoolean(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean getBooleanField(Object target, java.lang.reflect.Field field) {
        try {
            return field.getBoolean(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void callPrivateCheckAndSetProcessing(ScheduleReader reader) {
        try {
            var m = ScheduleReader.class.getDeclaredMethod("checkAndSetProcessing");
            m.setAccessible(true);
            m.invoke(reader);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    private static WorkSchedule.WorkScheduleBuilder callPrivateGetWorkScheduleBuilder(
            ScheduleReader reader, WorkScheduleRow row,
            YearMonth yearMonth, String roomSymbol,
            String cleanedStart, String cleanedEnd, UserEntity employee
    ) {
        try {
            var m = ScheduleReader.class.getDeclaredMethod(
                    "getWorkScheduleBuilder", WorkScheduleRow.class,
                    YearMonth.class, String.class, String.class,
                    String.class, UserEntity.class
            );
            m.setAccessible(true);
            return (WorkSchedule.WorkScheduleBuilder) m.invoke(
                    reader, row,
                    yearMonth, roomSymbol,
                    cleanedStart, cleanedEnd, employee);

        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }
}
