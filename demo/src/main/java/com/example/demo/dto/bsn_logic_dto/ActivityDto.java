package com.example.demo.dto.bsn_logic_dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDto {
    private Integer activityId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Warsaw")
    private Timestamp activityDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Warsaw")
    private Timestamp activityTime;

    private Integer employeeId;
    private String employeeCode;
    private String employeeFullName;
    private String procedureName;
    private String procedureType;
    private boolean isAssignedToLoggedUser;
    private boolean hasHistory;
    private boolean procedureScheduledOnEmployeesWorkingDay;
    private String roomCode;

    private List<String> employeesAssigned;

    // Dodane pole do rozróżniania jednoznacznie userów
    private List<Integer> employeeIdsAssigned;
}
