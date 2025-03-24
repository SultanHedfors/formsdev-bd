package com.example.demo.dto.bsn_logic_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDto {
    private Integer activityId;
    private Timestamp activityDate;

    private Timestamp activityTime;
    private Long employeeId;

    private String employeeFullName;
}
