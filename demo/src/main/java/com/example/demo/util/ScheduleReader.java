package com.example.demo.util;

import com.example.demo.entity.RoomEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ScheduledActivityToWSService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.demo.util.ExcelUtil.getCellValueAsString;
import static com.example.demo.util.ReportCreator.writeLogFile;
import static com.example.demo.util.TimeUtil.calculateDuration;
import static com.example.demo.util.TimeUtil.formatTime;

@Slf4j
@Component
public class ScheduleReader {

    protected static final Set<String> MODE_SET = Set.of("F", "B", "U", "UW", "ZL");
    public static final String EMPLOYEE_CODE_HEADER = "Kod pracownika";

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final RoomRepository roomRepository;
    private final ScheduledActivityToWSService scheduledActivityToWSService;

    public List<String> logMessages = new ArrayList<>();
    public List<String> validationErrors = new ArrayList<>();

    public ScheduleReader(UserRepository userRepository,
                          ScheduleRepository scheduleRepository,
                          RoomRepository roomRepository,
                          ScheduledActivityToWSService scheduledActivityToWSService) {
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.roomRepository = roomRepository;
        this.scheduledActivityToWSService = scheduledActivityToWSService;
    }

//    @Transactional
    public List<WorkSchedule> mapRowsToEntities(String filePath) {

        log.info("Attempting load of Excel file at: {}", filePath);

        List<WorkSchedule> workSchedules = new ArrayList<>();
        List<Integer> employeesRowsIndexes;
        File excelFile = new File(filePath);
        YearMonth yearMonth = null;

        try (FileInputStream fis = new FileInputStream(excelFile)) {

            if (!excelFile.exists()) {
                log.error("File does not exist at path: {}", filePath);
                throw new FileNotFoundException("File not found: " + filePath);
            }

            log.info("File exists, attempting to load workbook...");

            Workbook workbook = new XSSFWorkbook(fis);
            log.info("Workbook loaded successfully");

            Sheet sheet = workbook.getSheetAt(0);
            log.info("Sheet[0] loaded: name = {}", sheet.getSheetName());

            // #TODO: dynamic reading from filename or cell
            yearMonth = YearMonth.of(2025, 1);
            log.info("YearMonth manually set to: {}", yearMonth);

            List<String> employeesCodes = employeesCodes();
            log.info("Employee codes loaded: {}", employeesCodes);

            List<String> roomCodes = getAllRoomCodes();
            log.info("Room codes loaded: {}", roomCodes);

            employeesRowsIndexes = getRowsWithEmployees(sheet, employeesCodes);
            log.info("Employee rows found at indexes: {}", employeesRowsIndexes);

            validateFile(sheet, employeesCodes, roomCodes, employeesRowsIndexes, validationErrors);
            if (!validationErrors.isEmpty()) {
                log.warn("Validation errors found: {}", validationErrors);
                logMessages.addAll(validationErrors);
                writeLogFile(filePath, logMessages, false, validationErrors, excelFile.getName());
                throw new RuntimeException("Validation errors in uploaded file.");
            }

            logMessages.add("Employees were found in rows: " + ExcelUtil.getActualExcelIndexes(employeesRowsIndexes));

            for (Integer rowIndex : employeesRowsIndexes) {
                Row employeeRow = sheet.getRow(rowIndex);
                if (employeeRow == null) {
                    log.warn("Skipped null employee row at index {}", rowIndex);
                    continue;
                }

                Row aboveRow = (rowIndex > 0) ? sheet.getRow(rowIndex - 1) : null;
                String roomSymbol = extractRoomSymbol(aboveRow);

                Row workModeRow = sheet.getRow(rowIndex);
                Row startTimeRow = sheet.getRow(rowIndex + 1);
                Row endTimeRow = sheet.getRow(rowIndex + 2);

                String employeeName = getCellValueAsString(employeeRow.getCell(0)).trim();
                log.info("Processing employee: '{}' at row {}", employeeName, rowIndex);

                if (startTimeRow != null && endTimeRow != null) {
                    processWorkScheduleRows(
                            aboveRow,
                            workModeRow,
                            startTimeRow,
                            endTimeRow,
                            yearMonth,
                            employeeName,
                            roomSymbol,
                            workSchedules,
                            employeesCodes
                    );
                    log.info("Work schedule processed for employee: {}", employeeName);
                } else {
                    log.warn("Start or end time row is null for employee: {}", employeeName);
                }
            }

        } catch (Exception e) {
            log.error("Processing failed: {}", e.getMessage(), e);
            try {
                logMessages.add("Processing failed: " + e.getMessage());
                writeLogFile(filePath, logMessages, false, List.of("Processing failed: " + e.getMessage()), excelFile.getName());
            } catch (Exception logEx) {
                log.error("Failed to write error log file: {}", logEx.getMessage(), logEx);
            }
            throw new RuntimeException("Processing failed: " + e.getMessage());
        }

        if (yearMonth != null) {
            log.info("Deleting existing schedules for YearMonth: {}", yearMonth);
            scheduleRepository.deleteByYearMonth(yearMonth.toString());
        }

        log.info("Saving {} new work schedule entries", workSchedules.size());
        scheduleRepository.saveAll(workSchedules);

        log.info("Assigning scheduled activities to work schedules...");
        scheduledActivityToWSService.assignActivitiesToSchedules(true);

        logMessages.add("Processing completed successfully. Total employees processed: " + workSchedules.size());

        List<String> scheduleSummary = workSchedules.stream()
                .map(ws -> "YearMonth: " + ws.getYearMonth()
                        + ", Day: " + ws.getDayOfMonth()
                        + ", Employee: " + (ws.getEmployee() != null ? ws.getEmployee().getEmployeeCode() : "None")
                        + ", Start: " + ws.getWorkStartTime()
                        + ", End: " + ws.getWorkEndTime()
                        + ", Room: " + (ws.getRoomSymbol() != null ? ws.getRoomSymbol() : "None"))
                .toList();

        try {
            writeLogFile(filePath, logMessages, true, scheduleSummary, excelFile.getName());
            log.info("Log file written successfully.");
        } catch (Exception e) {
            log.error("Failed to write success log file: {}", e.getMessage(), e);
        }

        return workSchedules;
    }


    private String extractRoomSymbol(Row aboveRow) {
        if (aboveRow == null) return null;
        String val = getCellValueAsString(aboveRow.getCell(0)).trim();
        if (!val.equalsIgnoreCase("OK") && !val.isEmpty() && !val.equalsIgnoreCase(EMPLOYEE_CODE_HEADER)) {
            return val;
        }
        return null;
    }

    public void processWorkScheduleRows(Row aboveRow,
                                        Row workModeRow,
                                        Row startTimeRow,
                                        Row endTimeRow,
                                        YearMonth yearMonth,
                                        String employeeName,
                                        String roomSymbol,
                                        List<WorkSchedule> workSchedules,
                                        List<String> employeesCodes) {

        IntStream.range(1, 32).forEach(day -> {
            String dayBasedSubCode = null;
            if (aboveRow != null) {
                Cell aboveCell = aboveRow.getCell(day);
                String aboveVal = getCellValueAsString(aboveCell).trim().toUpperCase();
                if (employeesCodes.contains(aboveVal)) {
                    dayBasedSubCode = aboveVal;
                }
            }

            Cell workInfoCell = workModeRow.getCell(day);
            Cell startCell = startTimeRow.getCell(day);
            Cell endCell = endTimeRow.getCell(day);

            String startVal = (startCell != null) ? getCellValueAsString(startCell).trim() : "";
            String endVal = (endCell != null) ? getCellValueAsString(endCell).trim() : "";
            if (startVal.isEmpty() || endVal.isEmpty()) return;

            String cleanedStart = formatTime(startVal);
            String cleanedEnd = formatTime(endVal);

            if (cleanedStart == null || cleanedEnd == null) return;

            UserEntity employee = null;
            try {
                employee = findEmployeeByCode(employeeName);
            } catch (Exception e) {
                log.warn("Could not find employee with code '{}', skipping row. Day: {}, Start: {}, End: {}", employeeName, day, startVal, endVal);
                return;
            }


            WorkSchedule.WorkScheduleBuilder builder = WorkSchedule.builder()
                    .yearMonth(yearMonth.toString())
                    .dayOfMonth(day)
                    .employee(employee)
                    .workStartTime(cleanedStart)
                    .workEndTime(cleanedEnd)
                    .workDurationMinutes(calculateDuration(cleanedStart, cleanedEnd))
                    .roomSymbol(roomSymbol);

            if (dayBasedSubCode != null) {
                builder.substituteEmployee(findEmployeeByCode(dayBasedSubCode));
            }

            String infoVal = getCellValueAsString(workInfoCell).toUpperCase().trim();
            if (MODE_SET.contains(infoVal)) {
                builder.workMode(infoVal);
            } else if (employeesCodes.contains(infoVal)) {
                builder.substituteEmployee(findEmployeeByCode(infoVal));
                builder.workMode("S");
            } else {
                builder.workMode("S");
            }

            workSchedules.add(builder.build());
        });
    }

    private UserEntity findEmployeeByCode(String employeeCode) {
        return userRepository.findByEmployeeCode(employeeCode.toUpperCase()).orElseThrow();
    }

    private List<UserEntity> findAllEmployees() {
        return userRepository.findAll();
    }

    private List<String> employeesCodes() {
        return findAllEmployees().stream()
                .map(e -> e.getEmployeeCode().toUpperCase())
                .collect(Collectors.toList());
    }

    private List<Integer> getPopulatedRowsInFirstColumn(Sheet sheet) {
        List<Integer> rowIndexes = new ArrayList<>();
        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                rowIndexes.add(row.getRowNum());
            }
        }
        return rowIndexes;
    }

    private List<Integer> getRowsWithEmployees(Sheet sheet, List<String> employeesCodes) {
        return getPopulatedRowsInFirstColumn(sheet).stream()
                .filter(rowIndex -> {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        Cell cell = row.getCell(0);
                        if (cell != null) {
                            String cellValue = getCellValueAsString(cell).toUpperCase();
                            return employeesCodes.contains(cellValue);
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    private List<String> getAllRoomCodes() {
        return roomRepository.findAll().stream()
                .map(RoomEntity::getRoomCode)
                .collect(Collectors.toList());
    }

    private void validateFile(Sheet sheet, List<String> employeesCodes, List<String> roomCodes, List<Integer> employeesRowsIndexes, List<String> validationErrors) {
        ExcelValidateUtil.validateFirstColumnEntries(sheet, employeesCodes, roomCodes, validationErrors);
        ExcelValidateUtil.validateEmployeeRowEntries(sheet, employeesRowsIndexes, employeesCodes, validationErrors);
        ExcelValidateUtil.validateRoomRows(sheet, employeesCodes, roomCodes, validationErrors);
    }
}
