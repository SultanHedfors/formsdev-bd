package com.example.demo.service;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
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

@Service
@RequiredArgsConstructor
public class StatsExportService {

    private final DailyEmployeeStatisticRepository statsRepository;
    private final UserRepository userRepository;

    public File generateStatsXlsx(LocalDate from, LocalDate to) throws IOException {

        var stats = statsRepository.findAllByStartDayBetween(from, to);

        var employeeNames = userRepository.findAllById(stats
                        .stream()
                        .map(EmployeeDailyStatsEntity::getEmployeeId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));


        var groupedByEmployee = stats.stream()
                .collect(Collectors
                        .groupingBy(EmployeeDailyStatsEntity::getEmployeeId,
                        Collectors.toMap(EmployeeDailyStatsEntity::getStartDay, EmployeeDailyStatsEntity::getScore)));

        List<LocalDate> statsDates = stats.stream()
                .map(EmployeeDailyStatsEntity::getStartDay)
                .distinct()
                .sorted(Comparator.reverseOrder()) // najnowsze z lewej
                .toList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Statistics from " + from + " to " + to);

        CellStyle percentStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        percentStyle.setDataFormat(format.getFormat("0.00%"));

        int rowIdx = 1;

        // Nagłówek
        Row header = sheet.createRow(0);
        for (int i = 0; i < statsDates.size(); i++) {
            header.createCell(i + 1).setCellValue(statsDates.get(i).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }
        header.createCell(statsDates.size() + 1).setCellValue("Średnia dzienna pracownika");
        header.createCell(statsDates.size() + 2).setCellValue("Średnia dla okresu");

        for (var entry : groupedByEmployee.entrySet()) {
            Row row = sheet.createRow(rowIdx);
            String employeeName = employeeNames.getOrDefault(entry.getKey(), "Nieznany");
            row.createCell(0).setCellValue(employeeName);

            int employeeCount = 0;

            for (int i = 0; i < statsDates.size(); i++) {
                Double score = entry.getValue().get(statsDates.get(i));
                Cell cell = row.createCell(i + 1);
                if (score != null) {
                    cell.setCellValue(score); // score to już np. 0.35
                    cell.setCellStyle(percentStyle);
                    employeeCount++;
                } else {
                    cell.setCellValue("");
                }
            }

            // Średnia dzienna (formuła Excel)
            if (employeeCount > 0) {
                String startCol = CellReference.convertNumToColString(1);
                String endCol = CellReference.convertNumToColString(statsDates.size());
                String averageFormula = String.format("AVERAGEIF(%s%d:%s%d,\"<>\")", startCol, rowIdx + 1, endCol, rowIdx + 1);

                Cell avgCell = row.createCell(statsDates.size() + 1);
                avgCell.setCellFormula(averageFormula);
                avgCell.setCellStyle(percentStyle);
            }

            rowIdx++;
        }

        // Średnia globalna (dla wszystkich)
        Row totalRow = sheet.createRow(rowIdx);
        String startCol = CellReference.convertNumToColString(1);
        String endCol = CellReference.convertNumToColString(statsDates.size());
        String overallAverageFormula = String.format("AVERAGEIF(%s2:%s%d,\"<>\")", startCol, endCol, rowIdx);

        Cell avgAll1 = totalRow.createCell(statsDates.size() + 1);
        avgAll1.setCellFormula(overallAverageFormula);
        avgAll1.setCellStyle(percentStyle);

        Cell avgAll2 = totalRow.createCell(statsDates.size() + 2);
        avgAll2.setCellFormula(overallAverageFormula);
        avgAll2.setCellStyle(percentStyle);

        // Auto szerokość kolumn
        for (int i = 0; i < statsDates.size() + 3; i++) {
            sheet.autoSizeColumn(i);
        }

        // Zapis do pliku
        File file = Files.createTempFile("stats_", ".xlsx").toFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
            out.flush();
        }
        workbook.close();

        return file;
    }
}
