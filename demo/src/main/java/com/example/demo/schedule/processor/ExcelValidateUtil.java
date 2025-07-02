package com.example.demo.schedule.processor;

import com.example.demo.exception.ScheduleValidationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.schedule.processor.ReportCreator.writeLogFile;
import static com.example.demo.schedule.processor.ScheduleReader.MODE_SET;
import static com.example.demo.util.ExcelUtil.getCellValueAsString;

@Slf4j
@Component
public class ExcelValidateUtil {
    private final LogUtil logUtil;

    @Getter
    private final List<String> validationErrors;

    public void addValidationError(String error) {
        this.validationErrors.add(error);
    }

    public ExcelValidateUtil(LogUtil logUtil) {
        this.logUtil = logUtil;
        validationErrors = new ArrayList<>();
    }

    void checkFileExists(File excelFile, String filePath) throws FileNotFoundException {
        if (!excelFile.exists())
            throw new FileNotFoundException("File not found: " + filePath);
    }

    void handleValidationErrors(String filePath, String fileName) {
        if (!validationErrors.isEmpty()) {
            logUtil.getLogMessages().addAll(validationErrors);
            writeLogFile(filePath, logUtil.getLogMessages(), false, validationErrors, fileName);
            throw new ScheduleValidationException(validationErrors.toString());
        }
    }

    void handleCriticalFailure(Exception e, String filePath, String fileName, String message) {
        try {
            logUtil.addLogMessage(message + ": " + e.getMessage());
            writeLogFile(filePath, logUtil.getLogMessages(), false, List.of(message + ": " + e.getMessage()), fileName);
        } catch (Exception logEx) {
            log.error("Failed to write error log file: {}", logEx.getMessage(), logEx);
        }
        throw new ScheduleValidationException(message + ": " + e.getMessage());
    }

    void validateFile(Sheet sheet, List<String> employeesCodes, List<String> roomCodes, List<Integer> employeesRowsIndexes) {
        validateFirstColumnEntries(sheet, employeesCodes, roomCodes);
        validateEmployeeRowEntries(sheet, employeesRowsIndexes, employeesCodes);
        validateRoomRows(sheet, employeesCodes, roomCodes);
    }

    boolean isValidDay(YearMonth yearMonth, int day) {
        try {
            var date = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), day);
            return !date.isAfter(yearMonth.atEndOfMonth());
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean validateFirstColumnEntries(Sheet sheet, List<String> employeesCodes, List<String> roomNames) {
        DataFormatter dataFormatter = new DataFormatter();
        boolean headerFound = false;

        for (Row row : sheet) {
            Cell firstCell = row.getCell(0);
            String cellValue = (firstCell == null) ? "" : dataFormatter.formatCellValue(firstCell).trim();

            boolean headerJustFound = false;
            if (!headerFound && cellValue.equalsIgnoreCase(ScheduleReader.EMPLOYEE_CODE_HEADER)) {
                headerFound = true;
                headerJustFound = true;
            }

            if (!headerFound || headerJustFound ||
                    cellValue.isBlank() ||
                    employeesCodes.contains(cellValue.toUpperCase()) ||
                    cellValue.equalsIgnoreCase("OK") ||
                    roomNames.contains(cellValue.toUpperCase())) {
                continue;
            }

            String errorMsg = String.format(
                    "❌ Invalid entry in first column at row %d: '%s' is not an employee code or a room name.",
                    row.getRowNum() + 1, cellValue);
            addValidationError(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        if (!headerFound) {
            throw new IllegalStateException("❌ Header 'Kod pracownika' not found in the first column.");
        }
        return true;
    }

    protected void validateEmployeeRowEntries(Sheet sheet, List<Integer> employeesRowsIndexes, List<String> employeesCodes) {
        if (employeesRowsIndexes.isEmpty()) {
            String errorMsg = "No rows with employees codes were found. Schedule file is invalid.";
            addValidationError(errorMsg);
        }
        for (Integer rowIndex : employeesRowsIndexes) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            for (int colIndex = 1; colIndex < row.getLastCellNum(); colIndex++) {
                Cell cell = row.getCell(colIndex);
                String cellValue = getCellValueAsString(cell).trim().toUpperCase();

                if (cellValue.isEmpty()) continue;

                if (!employeesCodes.contains(cellValue) && !MODE_SET.contains(cellValue)) {
                    String errorMsg = String.format("❌ Invalid value at row %d, column %d: '%s'. It is neither a valid employee code nor a valid mode.",
                            rowIndex + 1, colIndex + 1, cellValue);
                    log.error(errorMsg);
                    addValidationError(errorMsg);
                }
            }
        }
    }

    public void validateRoomRows(Sheet sheet, List<String> employeesCodes, List<String> roomNames) {
        for (Row row : sheet) {
            Cell firstCell = row.getCell(0);
            if (firstCell == null) continue;

            String cellValue = getCellValueAsString(firstCell).trim().toUpperCase();

            if (roomNames.contains(cellValue)) {
                for (int colIndex = 1; colIndex < row.getLastCellNum(); colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    String value = getCellValueAsString(cell).trim().toUpperCase();

                    if (!value.isEmpty() && !employeesCodes.contains(value)) {
                        String errorMessage = String.format("❌ Invalid value in room row %d, column %d: '%s'. " +
                                "Only employee codes are allowed.", row.getRowNum() + 1, colIndex + 1, value);
                        addValidationError(errorMessage);
                        log.error(errorMessage);
                    }
                }
            }
        }
    }


}
