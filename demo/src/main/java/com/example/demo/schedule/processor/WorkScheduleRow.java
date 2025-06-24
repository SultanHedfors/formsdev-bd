package com.example.demo.schedule.processor;

import org.apache.poi.ss.usermodel.Row;

import java.util.List;

import static com.example.demo.util.ExcelUtil.getCellValueAsString;

public record WorkScheduleRow(
        int day,
        String dayBasedSubCode,
        String startVal,
        String endVal,
        String infoVal,
        List<String> employeesCodes
) {
    static WorkScheduleRow of(Row aboveRow, Row workModeRow, Row startTimeRow, Row endTimeRow, int day, List<String> employeesCodes) {
        String dayBasedSubCode = null;
        if (aboveRow != null) {
            var aboveVal = getCellValueAsString(aboveRow.getCell(day)).trim().toUpperCase();
            if (employeesCodes.contains(aboveVal)) dayBasedSubCode = aboveVal;
        }
        var startCell = startTimeRow.getCell(day);
        var endCell = endTimeRow.getCell(day);
        var startVal = (startCell != null) ? getCellValueAsString(startCell).trim() : "";
        var endVal = (endCell != null) ? getCellValueAsString(endCell).trim() : "";

        if (startVal.isEmpty() || endVal.isEmpty()) return null;

        var workInfoCell = workModeRow.getCell(day);
        var infoVal = getCellValueAsString(workInfoCell).toUpperCase().trim();
        return new WorkScheduleRow(day, dayBasedSubCode, startVal, endVal, infoVal, employeesCodes);
    }
}
