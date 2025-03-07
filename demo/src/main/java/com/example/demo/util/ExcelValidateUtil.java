package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

import static com.example.demo.util.ExcelUtil.getCellValueAsString;
import static com.example.demo.util.ScheduleReader.MODE_SET;

@Slf4j
public class ExcelValidateUtil {



    /**
     * Validates the first column of the sheet to ensure each entry is a valid employee code, "OK", or a room name.
     * Instead of throwing exceptions immediately, it collects validation errors and returns them for reporting.
     *
     * @param sheet         Excel sheet to validate
     * @param employeesCodes List of valid employee codes
     * @param roomNames     List of valid room names
     * @param validationErrors List to store validation errors
     */
    protected static void validateFirstColumnEntries(Sheet sheet, List<String> employeesCodes, List<String> roomNames, List<String> validationErrors) {
//        log.info("validation start");
        DataFormatter dataFormatter = new DataFormatter();
        boolean headerFound = false;

        for (Row row : sheet) {
            Cell firstCell = row.getCell(0);
            if (firstCell == null) continue;

            String cellValue = dataFormatter.formatCellValue(firstCell).trim();
            if (!headerFound && cellValue.equalsIgnoreCase(ScheduleReader.EMPLOYEE_CODE_HEADER)) {
                headerFound = true;
                continue;
            }

            if (!headerFound) continue;

            // Check if the value is valid
//            log.info("cellValue {} is a room name: {}",cellValue,roomNames.contains(cellValue.toUpperCase()));
            if (!cellValue.isBlank() && !employeesCodes.contains(cellValue.toUpperCase()) &&
                    !cellValue.equalsIgnoreCase("OK") &&
                    !roomNames.contains(cellValue.toUpperCase())) {
                String errorMsg = String.format("❌ Invalid entry in first column at row %d: '%s' is not an employee code or a room name.",
                        row.getRowNum() + 1, cellValue);
                log.error(errorMsg);
                validationErrors.add(errorMsg);
            }
        }
    }

    /**
     * Validates employee row entries to ensure only valid employee codes or work modes are used.
     * Instead of throwing exceptions immediately, it collects validation errors and returns them for reporting.
     *
     * @param sheet               Excel sheet to validate
     * @param employeesRowsIndexes List of employee row indexes
     * @param employeesCodes      List of valid employee codes
     * @param validationErrors    List to store validation errors
     */
    protected static void validateEmployeeRowEntries(Sheet sheet, List<Integer> employeesRowsIndexes, List<String> employeesCodes, List<String> validationErrors) {
        for (Integer rowIndex : employeesRowsIndexes) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            for (int colIndex = 1; colIndex < row.getLastCellNum(); colIndex++) {
                Cell cell = row.getCell(colIndex);
                String cellValue = getCellValueAsString(cell).trim().toUpperCase();

                // Allow empty values
                if (cellValue.isEmpty()) continue;

                // Check if value is in employeesCodes or MODE_SET
                if (!employeesCodes.contains(cellValue) && !MODE_SET.contains(cellValue)) {
                    String errorMsg = String.format("❌ Invalid value at row %d, column %d: '%s'. It is neither a valid employee code nor a valid mode.",
                            rowIndex + 1, colIndex + 1, cellValue);
                    log.error(errorMsg);
                    validationErrors.add(errorMsg);
                }
            }
        }
    }
    public static void validateRoomRows(Sheet sheet, List<String> employeesCodes, List<String> roomNames,
                                        List<String> validationMessages) {
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
                        validationMessages.add(errorMessage);
                        log.error(errorMessage);
                    }
                }
            }
        }
    }
}
