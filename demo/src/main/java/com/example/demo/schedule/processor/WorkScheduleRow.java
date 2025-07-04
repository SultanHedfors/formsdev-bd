package com.example.demo.schedule.processor;

import org.apache.poi.ss.usermodel.Row;

import java.util.Set;

import static com.example.demo.util.ExcelUtil.getCellValueAsString;

public record WorkScheduleRow(
        int day,
        String dayBasedSubCode,
        String startVal,
        String endVal,
        String infoVal,
        Set<String> employeesCodes
) {
    static WorkScheduleRow of(Row daysRow, Row workModeRow, Row startTimeRow, Row endTimeRow, int day, Set<String> employeesCodes) {
        String workMode = null;
        if (daysRow != null) {
            var aboveVal = getCellValueAsString(daysRow.getCell(day)).trim().toUpperCase();
            if (employeesCodes.contains(aboveVal)) workMode = aboveVal;
        }
        var startCell = startTimeRow.getCell(day);
        var endCell = endTimeRow.getCell(day);
        var startVal = (startCell != null) ? getCellValueAsString(startCell).trim() : "";
        var endVal = (endCell != null) ? getCellValueAsString(endCell).trim() : "";

        if (startVal.isEmpty() || endVal.isEmpty()) return null;

        var workModeCell = workModeRow.getCell(day);
        var infoVal = getCellValueAsString(workModeCell).toUpperCase().trim();
        return new WorkScheduleRow(day, workMode, startVal, endVal, infoVal, employeesCodes);
    }
}
