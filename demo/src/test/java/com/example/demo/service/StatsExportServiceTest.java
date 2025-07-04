package com.example.demo.service;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsExportServiceTest {

    private DailyEmployeeStatisticRepository statsRepository;
    private UserRepository userRepository;
    private StatsExportService service;

    @BeforeEach
    void setUp() {
        statsRepository = mock(DailyEmployeeStatisticRepository.class);
        userRepository = mock(UserRepository.class);
        service = new StatsExportService(statsRepository, userRepository);
    }

    @Test
    void generateStatsXlsx_createsXlsxFileWithStats() throws Exception {
        // Arrange
        LocalDate from = LocalDate.of(2024, 7, 1);
        LocalDate to = LocalDate.of(2024, 7, 3);

        var stat1 = new EmployeeDailyStatsEntity();
        stat1.setEmployeeId(1);
        stat1.setStartDay(LocalDate.of(2024, 7, 1));
        stat1.setScore(0.5);

        var stat2 = new EmployeeDailyStatsEntity();
        stat2.setEmployeeId(1);
        stat2.setStartDay(LocalDate.of(2024, 7, 2));
        stat2.setScore(0.8);

        var stat3 = new EmployeeDailyStatsEntity();
        stat3.setEmployeeId(2);
        stat3.setStartDay(LocalDate.of(2024, 7, 1));
        stat3.setScore(0.7);

        when(statsRepository.findAllByStartDayBetween(from, to)).thenReturn(List.of(stat1, stat2, stat3));

        var user1 = new UserEntity();
        user1.setId(1);
        user1.setFullName("Alice");
        var user2 = new UserEntity();
        user2.setId(2);
        user2.setFullName("Bob");

        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user1, user2));

        // Act
        File file = service.generateStatsXlsx(from, to);

        // Assert
        assertThat(file).exists().canRead().hasExtension("xlsx");

        try (FileInputStream fis = new FileInputStream(file); Workbook wb = new XSSFWorkbook(fis)) {
            var sheet = wb.getSheetAt(0);

            var header = sheet.getRow(0);
            assertThat(header.getCell(1).getStringCellValue()).contains("2024");

            var row1 = sheet.getRow(1);
            assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("Alice");
            assertThat(row1.getCell(1).getNumericCellValue()).isEqualTo(0.8);
            assertThat(row1.getCell(2).getNumericCellValue()).isEqualTo(0.5);
            assertThat(row1.getCell(3).getCellFormula()).isNotEmpty();

            var row2 = sheet.getRow(2);
            assertThat(row2.getCell(0).getStringCellValue()).isEqualTo("Bob");
            assertThat(row2.getCell(1).getStringCellValue()).isEmpty();
            assertThat(row2.getCell(2).getNumericCellValue()).isEqualTo(0.7);

        }

        boolean deleted = file.delete();
        assertThat(deleted).isTrue();
    }

    @Test
    void createHeaders_addsProperHeaders() throws Exception {
        // Arrange
        var dates = List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));

        // Act + Assert
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            StatsExportService.createHeaders(sheet, dates);
            var header = sheet.getRow(0);

            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("01-01-2024");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("02-01-2024");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Per employee daily average");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Average for period");
        }
    }

    @Test
    void createFile_createsPhysicalFile() throws Exception {
        // Arrange
        try (var workbook = new XSSFWorkbook()) {
            // Act
            File file = StatsExportService.createFile(workbook);

            // Assert
            assertThat(file).exists().canRead();
            boolean deleted = file.delete();
            assertThat(deleted).isTrue();
        }
    }
}
