package com.example.demo.schedule.processor;

import com.example.demo.entity.WorkSchedule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.example.demo.schedule.processor.ReportCreator.writeLogFile;
import static com.example.demo.util.ExcelUtil.getActualExcelIndexes;
@Getter
@Slf4j
@Component
public class LogUtil {
    private final List<String> logMessages;

    public LogUtil () {
        logMessages = new ArrayList<>();
    }

    public void addLogMessage(String message) {
        this.logMessages.add(message);
    }
    void logEmployeesRowPositions(List<Integer> employeesRowsIndexes) {
        addLogMessage("Employees were found in rows: " + getActualExcelIndexes(employeesRowsIndexes));
    }

    void logSuccess(ArrayList<WorkSchedule> workSchedules) {
        addLogMessage("Processing completed successfully. Total employees processed: " + workSchedules.size());
    }
     void writeSummaryLog(String filePath, List<WorkSchedule> workSchedules, String fileName) {
         var scheduleSummary = workSchedules.stream()
                 .map(ws -> String.format(
                         "YearMonth: %s, Day: %s, Employee: %s, Start: %s, End: %s, Room: %s",
                         ws.getYearMonth(),
                         ws.getDayOfMonth(),
                         ws.getEmployee() != null ? ws.getEmployee().getEmployeeCode() : "None",
                         ws.getWorkStartTime(),
                         ws.getWorkEndTime(),
                         ws.getRoomSymbol() != null ? ws.getRoomSymbol() : "None"
                 ))
                 .toList();

        try {
            writeLogFile(filePath, logMessages, true, scheduleSummary, fileName);
            log.info("Log file written successfully.");
        } catch (Exception e) {
            log.error("Failed to write log file: {}", e.getMessage(), e);
        }
    }
}
