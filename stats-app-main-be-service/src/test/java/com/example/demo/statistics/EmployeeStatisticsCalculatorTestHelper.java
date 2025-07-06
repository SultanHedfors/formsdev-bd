package com.example.demo.statistics;

import com.example.demo.entity.*;

import java.util.ArrayList;
import java.util.List;

public class EmployeeStatisticsCalculatorTestHelper {

    static List<UserEntity> employees = createEmployees();
    static List<ProcedureEntity> procedures = createProcedures();

    static List<UserEntity> createEmployees() {
        return List.of(
                UserEntity.builder().id(1).build(),
                UserEntity.builder().id(2).build(),
                UserEntity.builder().id(3).build(),
                UserEntity.builder().id(4).build()
        );
    }

    static List<ProcedureEntity> createProcedures() {
        return List.of(
                ProcedureEntity.builder().workMode("B").procedureActualTime(90).build(),
                ProcedureEntity.builder().workMode("F").procedureActualTime(120).build(),
                ProcedureEntity.builder().workMode("S").procedureActualTime(60).build(),
                ProcedureEntity.builder().workMode("U").procedureActualTime(10).build()
        );
    }

    static List<ActivityEntity> createActivities(List<ProcedureEntity> procedures) {
        List<ActivityEntity> activities = new ArrayList<>();
        int id = 10;
        // 3 activities to procedure B
        activities.add(ActivityEntity.builder().activityId(id++).procedure(procedures.get(0)).build());
        activities.add(ActivityEntity.builder().activityId(id++).procedure(procedures.get(0)).build());
        activities.add(ActivityEntity.builder().activityId(id++).procedure(procedures.get(0)).build());
        // 2 activities to procedure F
        activities.add(ActivityEntity.builder().activityId(id++).procedure(procedures.get(1)).build());
        activities.add(ActivityEntity.builder().activityId(id++).procedure(procedures.get(1)).build());
        // 1 activity to S & U
        activities.add(ActivityEntity.builder().activityId(id++).procedure(procedures.get(2)).build());
        activities.add(ActivityEntity.builder().activityId(id).procedure(procedures.get(3)).build());
        return activities;
    }

    static List<WorkSchedule> createWorkSchedules(List<UserEntity> employees) {
        List<WorkSchedule> schedules = new ArrayList<>();
        int id = 100;
        schedules.add(WorkSchedule.builder()
                .id(id++)
                .substituteEmployee(employees.get(0))
                .workMode("B")
                .workDurationMinutes(100)
                .build());
        schedules.add(WorkSchedule.builder()
                .id(id++)
                .substituteEmployee(employees.get(1))
                .workMode("B")
                .workDurationMinutes(95)
                .build());
        schedules.add(WorkSchedule.builder()
                .id(id++)
                .substituteEmployee(employees.get(2))
                .workMode("F")
                .workDurationMinutes(130)
                .build());
        schedules.add(WorkSchedule.builder()
                .id(id++)
                .substituteEmployee(employees.get(3))
                .workMode("F")
                .workDurationMinutes(150)
                .build());
        // S & U
        schedules.add(WorkSchedule.builder()
                .id(id++)
                .substituteEmployee(employees.get(0))
                .workMode("S")
                .workDurationMinutes(60)
                .build());
        schedules.add(WorkSchedule.builder()
                .id(id)
                .substituteEmployee(employees.get(1))
                .workMode("U")
                .workDurationMinutes(10)
                .build());
        return schedules;
    }

    static List<ActivityEmployeeEntity> createActivityEmployees() {
        var activities = createActivities(procedures);
        var schedules = createWorkSchedules(employees);

        List<ActivityEmployeeEntity> assignments = new ArrayList<>();

        // WorkMode = B
        assignments.add(ActivityEmployeeEntity.builder()
                .activity(activities.get(0)) // activityId=10, procedure=B
                .employee(employees.get(0))  // employeeId=1
                .workSchedule(schedules.get(0)) // workScheduleId=100
                .build());
        assignments.add(ActivityEmployeeEntity.builder()
                .activity(activities.get(1)) // activityId=11, procedure=B
                .employee(employees.get(1))  // employeeId=2
                .workSchedule(schedules.get(1)) // workScheduleId=101
                .build());
        assignments.add(ActivityEmployeeEntity.builder()
                .activity(activities.get(2)) // activityId=12, procedure=B
                .employee(employees.get(2))  // employeeId=3
                .workSchedule(schedules.get(0)) // workScheduleId=100
                .build());
        // WorkMode = F
        assignments.add(ActivityEmployeeEntity.builder()
                .activity(activities.get(3)) // activityId=13, procedure=F
                .employee(employees.get(2))  // employeeId=3
                .workSchedule(schedules.get(2)) // workScheduleId=102
                .build());
        assignments.add(ActivityEmployeeEntity.builder()
                .activity(activities.get(4)) // activityId=14, procedure=F
                .employee(employees.get(3))  // employeeId=4
                .workSchedule(schedules.get(3)) // workScheduleId=103
                .build());
        // WorkMode = S
        assignments.add(ActivityEmployeeEntity.builder()
                .activity(activities.get(5)) // activityId=15, procedure=S
                .employee(employees.get(0))  // employeeId=1
                .workSchedule(schedules.get(4)) // workScheduleId=104
                .build());
        // WorkMode = U
        assignments.add(ActivityEmployeeEntity.builder()
                .activity(activities.get(6)) // activityId=16, procedure=U
                .employee(employees.get(1))  // employeeId=2
                .workSchedule(schedules.get(5)) // workScheduleId=105
                .build());
        return assignments;
    }
}
