package com.example.demo.schedule.processor;

import com.example.demo.entity.RoomEntity;
import com.example.demo.entity.UserEntity;
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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleReaderHelperTest {

    private DailyEmployeeStatisticRepository dailyRepo;
    private WeeklyEmployeeStatisticRepository weeklyRepo;
    private MonthlyEmployeeStatisticRepository monthlyRepo;
    private YearlyEmployeeStatisticRepository yearlyRepo;
    private RoomRepository roomRepo;
    private UserRepository userRepo;
    private ScheduleRepository scheduleRepo;
    private ScheduleReaderHelper helper;

    @BeforeEach
    void setUp() {
        dailyRepo = mock(DailyEmployeeStatisticRepository.class);
        weeklyRepo = mock(WeeklyEmployeeStatisticRepository.class);
        monthlyRepo = mock(MonthlyEmployeeStatisticRepository.class);
        yearlyRepo = mock(YearlyEmployeeStatisticRepository.class);
        roomRepo = mock(RoomRepository.class);
        userRepo = mock(UserRepository.class);
        scheduleRepo = mock(ScheduleRepository.class);
        ProtoObjectsMapping protoObjectsMapping = mock(ProtoObjectsMapping.class);
        GrpcSendSchedulesClient grpcClient = mock(GrpcSendSchedulesClient.class);

        helper = new ScheduleReaderHelper(
                dailyRepo, weeklyRepo, monthlyRepo, yearlyRepo,
                roomRepo, userRepo, scheduleRepo, protoObjectsMapping, grpcClient
        );
    }

    @Test
    void employeesCodes_returnsAllCodesUppercase() {
        // Arrange
        UserEntity user1 = new UserEntity();
        user1.setEmployeeCode("abc");
        UserEntity user2 = new UserEntity();
        user2.setEmployeeCode("XyZ");
        when(userRepo.findAll()).thenReturn(List.of(user1, user2));

        // Act
        Set<String> codes = helper.employeesCodes();

        // Assert
        assertThat(codes).containsExactlyInAnyOrder("ABC", "XYZ");
    }

    @Test
    void getAllRoomCodes_returnsAllRoomCodes() {
        // Arrange
        RoomEntity room1 = new RoomEntity();
        room1.setRoomCode("101");
        RoomEntity room2 = new RoomEntity();
        room2.setRoomCode("LAB");
        when(roomRepo.findAll()).thenReturn(List.of(room1, room2));

        // Act
        Set<String> codes = helper.getAllRoomCodes();

        // Assert
        assertThat(codes).containsExactlyInAnyOrder("101", "LAB");
    }

    @Test
    void findEmployeeByCode_findsUserCaseInsensitive() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setEmployeeCode("ZZZ");
        when(userRepo.findByEmployeeCode("ZZZ")).thenReturn(Optional.of(user));

        // Act
        UserEntity result = helper.findEmployeeByCode("zzz");

        // Assert
        assertThat(result).isSameAs(user);
    }

    @Test
    void findEmployeeByCode_throwsIfNotFound() {
        // Arrange
        when(userRepo.findByEmployeeCode("AAA")).thenReturn(Optional.empty());

        // Assert
        assertThatThrownBy(() -> helper.findEmployeeByCode("aaa")).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void cleanOverwrittenTables_deletesAllData() {
        // Arrange
        YearMonth yearMonth = YearMonth.of(2023, 8);

        // Act
        helper.cleanOverwrittenTables(yearMonth);

        // Assert
        verify(scheduleRepo).deleteByYearMonth("2023-08");
        verify(monthlyRepo).deleteByMonth("2023-08");
        verify(weeklyRepo).deleteByWeekStartBetween(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        verify(dailyRepo).deleteByStartDayBetween(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        verify(yearlyRepo).deleteByYear(2023);
    }

    @Test
    void retrieveJwt_returnsToken() {
        // Act
        String result = helper.retrieveJwt("Bearer tokenValue123");

        // Assert
        assertThat(result).isEqualTo("tokenValue123");
    }

    @Test
    void retrieveJwt_throwsForInvalidHeader() {
        assertThatThrownBy(() -> helper.retrieveJwt("Basic something"))
                .isInstanceOf(InvalidAuthHeaderException.class);
        assertThatThrownBy(() -> helper.retrieveJwt(null))
                .isInstanceOf(InvalidAuthHeaderException.class);
    }

    @Test
    void getRowsWithEmployees_worksWithSheet() {
        // Arrange
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("JOHN");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("jane");
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("notEmployee");

            Set<String> employees = Set.of("JOHN", "JANE");

            // Act
            List<Integer> result = ScheduleReaderHelper.getRowsWithEmployees(sheet, employees);

            // Assert
            assertThat(result).containsExactlyInAnyOrder(0, 1);
        } catch (Exception e) {
            fail("Workbook creation failed", e);
        }
    }

    @Test
    void getLastPopulatedRow_returnsCorrectRow() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("A");
            sheet.createRow(1).createCell(0).setBlank();
            sheet.createRow(2).createCell(0).setCellValue("B");

            assertThat(ScheduleReaderHelper.getLastPopulatedRow(sheet)).isEqualTo(2);
        } catch (Exception e) {
            fail("Workbook creation failed", e);
        }
    }

    @Test
    void extractRoomSymbol_worksForNormalRow() {
        Row row = mock(Row.class);
        when(row.getCell(0)).thenReturn(mock(Cell.class));
        when(row.getCell(0).getCellType()).thenReturn(CellType.STRING);
        when(row.getCell(0).getStringCellValue()).thenReturn("ROOM1");

        assertThat(ScheduleReaderHelper.extractRoomSymbol(row)).isEqualTo("ROOM1");
    }

    @Test
    void extractRoomSymbol_returnsNullForHeaderOrOkOrEmpty() {
        Row row = mock(Row.class);
        Cell cell = mock(Cell.class);
        when(row.getCell(0)).thenReturn(cell);

        // empty
        when(cell.getCellType()).thenReturn(CellType.STRING);
        when(cell.getStringCellValue()).thenReturn("");
        assertThat(ScheduleReaderHelper.extractRoomSymbol(row)).isNull();

        // OK
        when(cell.getStringCellValue()).thenReturn("OK");
        assertThat(ScheduleReaderHelper.extractRoomSymbol(row)).isNull();

        // EMPLOYEE_CODE_HEADER
        when(cell.getStringCellValue()).thenReturn("employee_code");
        assertThat(ScheduleReaderHelper.extractRoomSymbol(row)).isEqualTo("employee_code");
    }
}
