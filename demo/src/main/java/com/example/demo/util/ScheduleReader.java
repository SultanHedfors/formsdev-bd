package com.example.demo.util;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
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
    private static final Set<String> MODE_SET = Set.of("F", "B", "U", "UW", "ZL");


    @Value("${schedule.filepath}")
    private String excelFilePath;

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    public ScheduleReader(UserRepository userRepository, ScheduleRepository scheduleRepository) {
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
    }

//    #TODO add a trigger for this method, currently works post bean construct
    @PostConstruct
    public List<WorkSchedule> mapRowsToEntities() {
        List<WorkSchedule> workSchedules = new ArrayList<>();
        try {
            File excelFile = ExcelUtil.getLatestExcelFile(excelFilePath);
            if (excelFile == null) {
                log.error("No Excel file found in the specified directory.");
                return null;
            }

            try (FileInputStream fis = new FileInputStream(excelFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);

//                #todo implement reading month and year reading (from file name probably)
                YearMonth yearMonth = YearMonth.of(2025, 1);

                List<String> employeesCodes = employeesCodes();
                List<Integer> employeesRowsIndexes = getRowsWithEmployees(sheet, employeesCodes);

                for (Integer rowIndex : employeesRowsIndexes) {
                    Row employeeRow = sheet.getRow(rowIndex);
                    if (employeeRow == null) continue;
                    Row aboveRow = (rowIndex - 1 >= 0) ? sheet.getRow(rowIndex - 1) : null;
                    String roomSymbol = null;
                    if (aboveRow != null) {
                        String val = getCellValueAsString(aboveRow.getCell(0)).trim();
                        if (!val.equalsIgnoreCase("OK") && !val.isEmpty()) {
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
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        scheduleRepository.saveAll(workSchedules);
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

}
