package com.example.demo.schedule.processor;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LateWorkSchedulesHandlerTest {

    @Test
    void addLateWorkSchedulesForLatestEndTime_addsLateScheduleWhenMissing() {
        // Arrange
        var handler = new LateWorkSchedulesHandler();
        var emp = new UserEntity();
        emp.setId(1);
        var ws1 = WorkSchedule.builder()
                .yearMonth("2024-07")
                .dayOfMonth(5)
                .employee(emp)
                .workMode("F")
                .workStartTime("09:00")
                .workEndTime("16:00")
                .build();
        var ws2 = WorkSchedule.builder()
                .yearMonth("2024-07")
                .dayOfMonth(5)
                .employee(emp)
                .workMode("F")
                .workStartTime("16:00")
                .workEndTime("20:00")
                .build();

        List<WorkSchedule> schedules = new ArrayList<>(List.of(ws1, ws2));

        // Act
        handler.addLateWorkSchedulesForLatestEndTime(schedules, YearMonth.of(2024, 7));

        // Assert
        var found = schedules.stream()
                .filter(w -> w.getWorkMode().equals("F"))
                .filter(w -> w.getWorkStartTime().equals("20:00:01"))
                .findAny();
        assertThat(found).isPresent();
        assertThat(found.get().getWorkEndTime()).isEqualTo("23:59:59");
        assertThat(found.get().getEmployee()).isSameAs(emp);
        assertThat(found.get().getWorkDurationMinutes()).isZero();
        assertThat(found.get().getRoomSymbol()).isNull();
        assertThat(found.get().getProcessed()).isFalse();
    }

    @Test
    void addLateWorkSchedulesForLatestEndTime_doesNotDuplicateLateSchedule() {
        // Arrange
        var handler = new LateWorkSchedulesHandler();
        var emp = new UserEntity();
        emp.setId(2);
        var ws1 = WorkSchedule.builder()
                .yearMonth("2024-07")
                .dayOfMonth(8)
                .employee(emp)
                .workMode("B")
                .workStartTime("10:00")
                .workEndTime("23:59:59")
                .build();
        var existingLate = WorkSchedule.builder()
                .yearMonth("2024-07")
                .dayOfMonth(8)
                .employee(emp)
                .workMode("B")
                .workStartTime("10:00")
                .workEndTime("23:59:59")
                .build();

        List<WorkSchedule> schedules = new ArrayList<>(List.of(ws1, existingLate));

        // Act
        handler.addLateWorkSchedulesForLatestEndTime(schedules, YearMonth.of(2024, 8));

        // Assert
        long count = schedules.stream()
                .filter(w -> w.getWorkMode().equals("B"))
                .filter(w -> w.getDayOfMonth() == 8)
                .count();
        assertThat(count).isEqualTo(2);
    }

    @Test
    void addLateWorkSchedulesForLatestEndTime_handlesSubstituteEmployee() {
        // Arrange
        var handler = new LateWorkSchedulesHandler();
        var emp = new UserEntity();
        emp.setId(3);
        var sub = new UserEntity();
        sub.setId(4);

        var ws = WorkSchedule.builder()
                .yearMonth("2024-07")
                .dayOfMonth(10)
                .employee(emp)
                .substituteEmployee(sub)
                .workMode("F")
                .workStartTime("12:00")
                .workEndTime("17:00")
                .build();

        List<WorkSchedule> schedules = new ArrayList<>(List.of(ws));

        // Act
        handler.addLateWorkSchedulesForLatestEndTime(schedules, YearMonth.of(2024, 7));

        // Assert
        var found = schedules.stream()
                .filter(w -> w.getWorkMode().equals("F"))
                .filter(w -> w.getWorkStartTime().equals("17:00:01"))
                .filter(w -> w.getEmployee().getId() == 4)
                .findAny();
        assertThat(found).isPresent();
    }

    @Test
    void addLateWorkSchedulesForLatestEndTime_noSchedules_noException() {
        var handler = new LateWorkSchedulesHandler();
        List<WorkSchedule> schedules = new ArrayList<>();
        handler.addLateWorkSchedulesForLatestEndTime(schedules, YearMonth.of(2024, 7));
        assertThat(schedules).isEmpty();
    }
}
