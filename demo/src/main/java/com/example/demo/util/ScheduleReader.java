package com.example.demo.util;

import com.example.demo.entity.RoomEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.demo.util.ExcelUtil.getCellValueAsString;
import static com.example.demo.util.ReportCreator.writeLogFile;
import static com.example.demo.util.TimeUtil.calculateDuration;
import static com.example.demo.util.TimeUtil.formatTime;

@Slf4j
@Component
public class ScheduleReader {

    //    F -fizyko,
//    B-Balneo,
//    U-praca100% czasu,
//    UW-urlop(czas nie liczony),
//    ZL-zwolnienie(czas nie liczony)
    protected static final Set<String> MODE_SET = Set.of("F", "B", "U", "UW", "ZL");

    public static final String EMPLOYEE_CODE_HEADER="Kod pracownika";

    @Value("${schedule.filepath}")
    private String excelFilePath;

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final RoomRepository roomRepository;

    public List<String> logMessages = new ArrayList<>();
    List<String> validationErrors = new ArrayList<>();

    public ScheduleReader(UserRepository userRepository, ScheduleRepository scheduleRepository,
                          RoomRepository roomRepository) {
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.roomRepository = roomRepository;
    }
    //    #TODO add a trigger for this method, currently works post bean construct
//    @PostConstruct
    public List<WorkSchedule> mapRowsToEntities() {
        log.info("Attempting load of Excel file at: {}", excelFilePath);

        List<WorkSchedule> workSchedules = new ArrayList<>();
        List<Integer> employeesRowsIndexes = null;
        File excelFile = null;
        YearMonth yearMonth = null;
        try {
            excelFile = ExcelUtil.getLatestExcelFile(excelFilePath);
            if (excelFile == null) {
                String errorMsg = "No Excel file found in the specified directory: " + excelFilePath;
                log.error(errorMsg);
                logMessages.add(errorMsg);
                writeLogFile(excelFilePath, logMessages, false, workSchedules, "File not found");
                return null;
            }
            try (FileInputStream fis = new FileInputStream(excelFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);

//                #todo implement reading month and year reading (from file name probably)
                yearMonth = YearMonth.of(2025, 1);

                List<String> employeesCodes = employeesCodes();
                List<String> roomCodes = getAllRoomCodes();
                employeesRowsIndexes = getRowsWithEmployees(sheet, employeesCodes);

                validateFile(sheet, employeesCodes, roomCodes, employeesRowsIndexes, validationErrors);
                // If there are validation errors, log them and stop processing
                if (!validationErrors.isEmpty()) {
                    logMessages.addAll(validationErrors);
                    writeLogFile(excelFile.getAbsolutePath(), logMessages, false, workSchedules, excelFile.getName());
                    return null;
                }

                logMessages.add("Employees were found in rows: " + ExcelUtil.getActualExcelIndexes(employeesRowsIndexes));

                for (Integer rowIndex : employeesRowsIndexes) {
                    Row employeeRow = sheet.getRow(rowIndex);
                    if (employeeRow == null) continue;
                    Row aboveRow = (rowIndex - 1 >= 0) ? sheet.getRow(rowIndex - 1) : null;
                    String roomSymbol = null;
                    if (aboveRow != null) {
                        String val = getCellValueAsString(aboveRow.getCell(0)).trim();
                        log.info("room v: {} equals? {} header: {} ", val, val.equalsIgnoreCase(EMPLOYEE_CODE_HEADER),EMPLOYEE_CODE_HEADER);
                        if (!val.equalsIgnoreCase("OK") && !val.isEmpty() && !val.equalsIgnoreCase(EMPLOYEE_CODE_HEADER)) {
                            roomSymbol = val;
                        }
                    }

                    Row workModeRow = sheet.getRow(rowIndex);
                    Row startTimeRow = sheet.getRow(rowIndex + 1);
                    Row endTimeRow = sheet.getRow(rowIndex + 2);

                    // Name
                    String employeeName = getCellValueAsString(employeeRow.getCell(0)).trim();

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
                    }
                }

            } catch (Exception e) {
                log.error("Processing failed: {}", e.getMessage());
                logMessages.add("Processing failed: " + e.getMessage());
                writeLogFile(excelFile.getAbsolutePath(), logMessages, false, workSchedules, excelFile.getName());
                return null;
            }
        } catch (Exception e) {
            log.error("Unexpected failure: {}", e.getMessage());
            logMessages.add("Unexpected failure: " + e.getMessage());
            assert excelFile != null;
            writeLogFile(excelFilePath, logMessages, false, workSchedules, excelFile.getName());
        }

        if (yearMonth != null) {
            scheduleRepository.deleteByYearMonth(yearMonth.toString());
        }
        scheduleRepository.saveAll(workSchedules);
        assert employeesRowsIndexes != null;
        logMessages.add("Processing completed successfully. Total employees processed: " + employeesRowsIndexes.size());
        writeLogFile(excelFilePath, logMessages, true, workSchedules, excelFile.getName());

        return workSchedules;
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

            // Build schedule
            WorkSchedule.WorkScheduleBuilder builder = WorkSchedule.builder()
                    .yearMonth(yearMonth.toString())
                    .dayOfMonth(day)
                    .employee(findEmployeeByCode(employeeName))
                    .workStartTime(cleanedStart)
                    .workEndTime(cleanedEnd)
                    .workDurationMinutes(calculateDuration(cleanedStart, cleanedEnd))
                    .roomSymbol(roomSymbol);

            if (dayBasedSubCode != null) {
                builder.substituteEmployee(findEmployeeByCode(dayBasedSubCode));
            }
            // Work mode logic
            String infoVal = getCellValueAsString(workInfoCell).toUpperCase().trim();
            if (MODE_SET.contains(infoVal)) {
                builder.workMode(infoVal);
            } else if (employeesCodes.contains(infoVal)) {
                builder.substituteEmployee(findEmployeeByCode(infoVal));

//                todo should work mode be allowed to be null?
                builder.workMode("no");
            } else {
                builder.workMode("no");
            }
            WorkSchedule schedule = builder.build();

            log.info("Saving schedule entry: {}", schedule.getEmployee());
            workSchedules.add(schedule);
        });
    }

    private UserEntity findEmployeeByCode(String employeeCode) {
        return userRepository.findByEmployeeCode(employeeCode.toUpperCase()).orElseThrow();
    }

    private List<UserEntity> findAllEmployees() {
        return userRepository.findAll();
    }

    private List<String> employeesCodes() {
        List<UserEntity> employeesEntities = findAllEmployees();
        return employeesEntities.stream()
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
        List<Integer> populatedRows = getPopulatedRowsInFirstColumn(sheet);

        return populatedRows.stream()
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
        List<RoomEntity> rooms = roomRepository.findAll();
        return rooms.stream().map(RoomEntity::getRoomCode).collect(Collectors.toList());
    }

    private void validateFile(Sheet sheet, List<String> employeesCodes, List<String> roomCodes, List<Integer> employeesRowsIndexes, List<String> validationErrors) {
        ExcelValidateUtil.validateFirstColumnEntries(sheet, employeesCodes, roomCodes, validationErrors);
        ExcelValidateUtil.validateEmployeeRowEntries(sheet, employeesRowsIndexes, employeesCodes, validationErrors);
        ExcelValidateUtil.validateRoomRows(sheet, employeesCodes, roomCodes, validationErrors);
    }
}
