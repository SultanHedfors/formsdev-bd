package com.example.demo.schedule.processor;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.ScheduledActivityToWSService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import static com.example.demo.util.ExcelUtil.getCellValueAsString;
import static com.example.demo.schedule.processor.ScheduleReaderHelper.*;
import static com.example.demo.util.TimeUtil.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleReader {
    private YearMonth yearMonth;
    protected static final Set<String> MODE_SET = Set.of("F", "B", "U", "UW", "ZL");
    public static final String EMPLOYEE_CODE_HEADER = "Kod pracownika";

    private volatile boolean cancelled = false;
    private volatile boolean processing = false;

    private final LateWorkSchedulesHandler lateSchedulesHandler;
    private final ScheduleRepository scheduleRepository;
    private final ScheduledActivityToWSService scheduledActivityToWSService;
    private final ScheduleReaderHelper helper;
    private final LogUtil logUtil;
    private final ExcelValidateUtil validateUtil;


    @Transactional
    public void mapRowsToEntities(String filePath) {
        checkAndSetProcessing();

        var workSchedules = new ArrayList<WorkSchedule>();
        File excelFile = new File(filePath);

        try (var fis = new FileInputStream(excelFile)) {
            var sheet = loadAndValidateExcelSheet(fis, excelFile, filePath);
            List<String> employeesCodes = helper.employeesCodes();
            var employeesRowsIndexes = getRowsWithEmployees(sheet, employeesCodes);

            logUtil.logEmployeesRowPositions(employeesRowsIndexes);

            processAllEmployees(sheet, employeesRowsIndexes, employeesCodes, workSchedules);

            helper.cleanOverwrittenTables(yearMonth);
            lateSchedulesHandler.addLateWorkSchedulesForLatestEndTime(workSchedules, yearMonth);

        } catch (FileNotFoundException e) {
            validateUtil.handleCriticalFailure(e, filePath, excelFile.getName(), "Nie znaleziono pliku");
        } catch (Exception e) {
            validateUtil.handleCriticalFailure(e, filePath, excelFile.getName(), "Processing failed");
        } finally {
            processing = false;
        }

        scheduleRepository.saveAll(workSchedules);

//        async grpc service invocation
        helper.sendSchedulesToReportCreator(workSchedules);

        if (cancelled) throwCancelled();

        scheduledActivityToWSService.assignActivitiesToSchedules(true, yearMonth.toString());
        logUtil.logSuccess(workSchedules);
        logUtil.writeSummaryLog(filePath, workSchedules, excelFile.getName());
    }

    private Sheet loadAndValidateExcelSheet(FileInputStream fis, File excelFile, String filePath) throws Exception {
        validateUtil.checkFileExists(excelFile, filePath);
        var workbook = new XSSFWorkbook(fis);
        var sheet = workbook.getSheetAt(0);

        yearMonth = parseYearMonthFromFileName(excelFile.getName());

        List<String> employeesCodes = helper.employeesCodes();
        var roomCodes = helper.getAllRoomCodes();
        var employeesRowsIndexes = getRowsWithEmployees(sheet, employeesCodes);

        validateUtil.validateFile(sheet, employeesCodes, roomCodes, employeesRowsIndexes);
        validateUtil.handleValidationErrors(filePath, excelFile.getName());

        return sheet;
    }

    private void checkAndSetProcessing() {
        if (processing) throw new IllegalStateException("Grafik już jest w trakcie przetwarzania.");
        processing = true;
        cancelled = false;
    }

    private void processAllEmployees(
            Sheet sheet,
            List<Integer> employeesRowsIndexes,
            List<String> employeesCodes,
            List<WorkSchedule> workSchedules
    ) {
        for (var rowIndex : employeesRowsIndexes) {
            if (cancelled) throwCancelled();
            processEmployeeRow(sheet, rowIndex, yearMonth, workSchedules, employeesCodes);
        }
    }
    private void processEmployeeRow(
            Sheet sheet,
            int rowIndex,
            YearMonth yearMonth,
            List<WorkSchedule> workSchedules,
            List<String> employeesCodes
    ) {
        //Ustawienie zmiennych
        var employeeRow = sheet.getRow(rowIndex);
        if (employeeRow == null) return;
        var aboveRow = (rowIndex > 0) ? sheet.getRow(rowIndex - 1) : null;
        var roomSymbol = extractRoomSymbol(aboveRow);
        var startTimeRow = sheet.getRow(rowIndex + 1);
        var endTimeRow = sheet.getRow(rowIndex + 2);
        var employeeName = getCellValueAsString(employeeRow.getCell(0)).trim();

        //Wywolanie metody process
        if (startTimeRow != null && endTimeRow != null) {
            processWorkScheduleRows(
                    aboveRow,
                    employeeRow,
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

    public void processWorkScheduleRows(
            Row aboveRow,
            Row workModeRow,
            Row startTimeRow,
            Row endTimeRow,
            YearMonth yearMonth,
            String employeeName,
            String roomSymbol,
            List<WorkSchedule> workSchedules,
            List<String> employeesCodes
    ) {
        IntStream.range(1, 32)
                .filter(day -> validateUtil.isValidDay(yearMonth, day))
                .mapToObj(day -> WorkScheduleRow.of(aboveRow, workModeRow, startTimeRow, endTimeRow, day, employeesCodes))
                .filter(Objects::nonNull)
                .forEach(row -> addWorkSchedule(row, yearMonth, employeeName, roomSymbol, workSchedules));
    }

    private void addWorkSchedule(
            WorkScheduleRow row,
            YearMonth yearMonth,
            String employeeName,
            String roomSymbol,
            List<WorkSchedule> workSchedules
    ) {
        var cleanedStart = formatTime(row.startVal());
        var cleanedEnd = formatTime(row.endVal());
        if (cleanedStart == null || cleanedEnd == null) return;

        UserEntity employee = helper.findEmployeeByCode(employeeName);

        WorkSchedule.WorkScheduleBuilder builder = getWorkScheduleBuilder(row, yearMonth, roomSymbol, cleanedStart, cleanedEnd, employee);

        workSchedules.add(builder.build());
    }


    private WorkSchedule.WorkScheduleBuilder getWorkScheduleBuilder(WorkScheduleRow row, YearMonth yearMonth, String roomSymbol, String cleanedStart, String cleanedEnd, UserEntity employee) {
        var builder = WorkSchedule.builder()
                .yearMonth(yearMonth.toString())
                .dayOfMonth(row.day())
                .employee(employee)
                .workStartTime(cleanedStart)
                .workEndTime(cleanedEnd)
                .workDurationMinutes(calculateDuration(cleanedStart, cleanedEnd))
                .roomSymbol(roomSymbol);

        if (row.dayBasedSubCode() != null) {
            builder.substituteEmployee(helper.findEmployeeByCode(row.dayBasedSubCode()));
        }

        String infoVal = row.infoVal();
        if (MODE_SET.contains(infoVal)) {
            builder.workMode(infoVal);
        } else if (row.employeesCodes().contains(infoVal)) {
            builder.substituteEmployee(helper.findEmployeeByCode(infoVal));
            builder.workMode("S");
        } else {
            builder.workMode("S");
        }
        return builder;
    }
    public void cancelProcessing() {
        this.cancelled = true;
        log.warn(">>> Przetwarzanie grafiku zostało anulowane na żądanie użytkownika.");
    }
}

