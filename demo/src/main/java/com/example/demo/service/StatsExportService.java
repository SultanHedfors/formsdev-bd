package com.example.demo.service;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.demo.util.ExcelUtil.*;

@Service
@RequiredArgsConstructor
public class StatsExportService {

    private final DailyEmployeeStatisticRepository statsRepository;
    private final UserRepository userRepository;

    public File generateStatsXlsx(LocalDate from, LocalDate to) throws IOException {
        var stats = statsRepository.findAllByStartDayBetween(from, to);

        var employeeNames = userRepository.findAllById(
                stats.stream().map(EmployeeDailyStatsEntity::getEmployeeId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));

        var groupedByEmployee = stats.stream()
                .collect(Collectors.groupingBy(EmployeeDailyStatsEntity::getEmployeeId,
                        Collectors.toMap(EmployeeDailyStatsEntity::getStartDay, EmployeeDailyStatsEntity::getScore)));

        var statsDates = stats.stream()
                .map(EmployeeDailyStatsEntity::getStartDay)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        File file;
        try (var workbook = new XSSFWorkbook()) {
            var percentStyle = formattedPercentStyle(workbook);
            var sheet = workbook.createSheet("Statistics from " + from + " to " + to);

            createHeaders(sheet, statsDates);

            int rowIdx = 1;
            for (var entry : groupedByEmployee.entrySet()) {
                var row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(employeeNames.getOrDefault(entry.getKey(), "Unknown employee"));

                boolean hasScore = false;

                //populate stats
                for (int i = 0; i < statsDates.size(); i++) {
                    var score = entry.getValue().get(statsDates.get(i));
                    var cell = row.createCell(i + 1);
                    if (score != null) {
                        cell.setCellValue(score);
                        cell.setCellStyle(percentStyle);
                        hasScore = true;
                    } else {
                        cell.setCellValue("");
                    }
                }
                createPerEmployeeAverage(hasScore, row, statsDates, rowIdx, percentStyle);
                rowIdx++;
            }
            createOverallAverages(sheet, rowIdx, statsDates, percentStyle);
            setColumnSize(statsDates, sheet);

            file = createFile(workbook);
        }
        return file;
    }


    static void createPerEmployeeAverage(boolean hasScore, XSSFRow row, List<LocalDate> statsDates, int rowIdx, CellStyle percentStyle) {
        if (hasScore) {
            var avgCell = row.createCell(statsDates.size() + 1);
            avgCell.setCellFormula(averageFormula(statsDates, rowIdx));
            avgCell.setCellStyle(percentStyle);
        }
    }

    static void createHeaders(Sheet sheet, List<LocalDate> statsDates) {
        var header = sheet.createRow(0);
        for (int i = 0; i < statsDates.size(); i++) {
            header.createCell(i + 1).setCellValue(statsDates.get(i).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }
        header.createCell(statsDates.size() + 1).setCellValue("Per employee daily average");
        header.createCell(statsDates.size() + 2).setCellValue("Average for period");
    }


    static void createOverallAverages(Sheet sheet, int rowIdx, List<LocalDate> statsDates, CellStyle percentStyle) {
        var totalRow = sheet.createRow(rowIdx);
        var overallAverageFormula = totalAverageFormula(statsDates, rowIdx);
        for (int i = statsDates.size() + 1; i <= statsDates.size() + 2; i++) {
            var cell = totalRow.createCell(i);
            cell.setCellFormula(overallAverageFormula);
            cell.setCellStyle(percentStyle);
        }
    }

    static File createFile(Workbook workbook) throws IOException {
        File file = Files.createTempFile("stats_", ".xlsx").toFile();
        try (var out = new FileOutputStream(file)) {
            workbook.write(out);
        }
        workbook.close();
        return file;
    }

}
