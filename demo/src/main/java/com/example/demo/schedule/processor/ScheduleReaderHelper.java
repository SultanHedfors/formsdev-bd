package com.example.demo.schedule.processor;

import com.example.demo.entity.RoomEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.exception.InvalidAuthHeaderException;
import com.example.demo.grpc.GrpcSendSchedulesClient;
import com.example.demo.mapper.ProtoObjectsMapping;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import com.example.demo.repository.stats.MonthlyEmployeeStatisticRepository;
import com.example.demo.repository.stats.WeeklyEmployeeStatisticRepository;
import com.example.demo.repository.stats.YearlyEmployeeStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

import static com.example.demo.schedule.processor.ScheduleReader.EMPLOYEE_CODE_HEADER;
import static com.example.demo.util.ExcelUtil.getCellValueAsString;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduleReaderHelper {
    private final DailyEmployeeStatisticRepository dailyEmployeeStatisticRepository;
    private final WeeklyEmployeeStatisticRepository weeklyEmployeeStatisticRepository;
    private final MonthlyEmployeeStatisticRepository monthlyEmployeeStatisticRepository;
    private final YearlyEmployeeStatisticRepository yearlyEmployeeStatisticRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProtoObjectsMapping protoObjectsMapping;
    private final GrpcSendSchedulesClient grpcClient;


    private List<UserEntity> findAllEmployees() {
        return userRepository.findAll();
    }

    List<String> employeesCodes() {
        return findAllEmployees().stream()
                .map(e -> e.getEmployeeCode().toUpperCase())
                .toList();
    }

    List<String> getAllRoomCodes() {
        return roomRepository.findAll().stream()
                .map(RoomEntity::getRoomCode)
                .toList();
    }

    UserEntity findEmployeeByCode(String employeeCode) {
        return userRepository.findByEmployeeCode(employeeCode.toUpperCase()).orElseThrow();
    }

    void cleanOverwrittenTables(YearMonth yearMonth) {
        log.info("Deleting existing schedules for YearMonth: {}", yearMonth);
        scheduleRepository.deleteByYearMonth(String.valueOf(yearMonth));
        monthlyEmployeeStatisticRepository.deleteByMonth(yearMonth.toString());
        deleteWeeklyStatsForMonth(yearMonth);
        deleteDailyStatsForMonth(yearMonth);
        yearlyEmployeeStatisticRepository.deleteByYear(yearMonth.getYear());
    }

    void deleteDailyStatsForMonth(YearMonth yearMonth) {
        var firstDayOfMonth = yearMonth.atDay(1);
        var lastDayOfMonth = yearMonth.atEndOfMonth();
        dailyEmployeeStatisticRepository.deleteByStartDayBetween(firstDayOfMonth, lastDayOfMonth);
    }

    void deleteWeeklyStatsForMonth(YearMonth yearMonth) {
        var firstDayOfMonth = yearMonth.atDay(1);
        var lastDayOfMonth = yearMonth.atEndOfMonth();
        weeklyEmployeeStatisticRepository.deleteByWeekStartBetween(firstDayOfMonth, lastDayOfMonth);
    }

    //util static methods
    static List<Integer> getRowsWithEmployees(Sheet sheet, List<String> employeesCodes) {
        var lastPopulatedRow = getLastPopulatedRow(sheet);
        return IntStream.rangeClosed(0, lastPopulatedRow)
                .filter(rowIndex -> {
                    var row = sheet.getRow(rowIndex);
                    if (row == null) return false;
                    var cell = row.getCell(0);
                    if (cell == null) return false;
                    var cellValue = getCellValueAsString(cell).toUpperCase();
                    return employeesCodes.contains(cellValue);
                })
                .boxed()
                .toList();
    }

    static int getLastPopulatedRow(Sheet sheet) {
        var lastRowNum = sheet.getLastRowNum();
        while (lastRowNum >= 0) {
            var row = sheet.getRow(lastRowNum);
            if (row != null && row.getCell(0) != null && row.getCell(0).getCellType() != CellType.BLANK) {
                return lastRowNum;
            }
            lastRowNum--;
        }
        return -1;
    }

    static String extractRoomSymbol(Row aboveRow) {
        if (aboveRow == null) return null;
        var val = getCellValueAsString(aboveRow.getCell(0)).trim();
        if (!val.equalsIgnoreCase("OK") && !val.isEmpty() && !val.equalsIgnoreCase(EMPLOYEE_CODE_HEADER)) {
            return val;
        }
        return null;
    }

    static void throwCancelled() {
        log.warn(">>> Przetwarzanie zostało przerwane w trakcie.");
        throw new RuntimeException("Przetwarzanie zostało anulowane przez użytkownika.");
    }

    String retrieveJwt(String authorizationHeader) {
        String jwt;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        } else {
            throw new InvalidAuthHeaderException();
        }
        return jwt;
    }

    @Async
    public void sendSchedulesToReportCreator(List<WorkSchedule> workSchedules, String jwt) {
        var protoScheduleObjects = protoObjectsMapping.scheduleEntityToProtoObjMapper(workSchedules);
        grpcClient.sendSchedulesRequest(protoScheduleObjects, jwt);
    }
}
