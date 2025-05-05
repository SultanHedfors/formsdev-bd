package com.example.demo.service;

import com.example.demo.entity.UserEntity;
import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
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

    public File generateXlsx(LocalDate from, LocalDate to) throws IOException {
        List<EmployeeDailyStatsEntity> stats = statsRepository.findAllByStartDayBetween(from, to);
        Map<Integer, String> employeeNames = userRepository.findAllById(
                stats.stream().map(EmployeeDailyStatsEntity::getEmployeeId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));

        // Grupa po employeeId
        Map<Integer, Map<LocalDate, Double>> grouped = stats.stream()
                .collect(Collectors.groupingBy(EmployeeDailyStatsEntity::getEmployeeId,
                        Collectors.toMap(EmployeeDailyStatsEntity::getStartDay, EmployeeDailyStatsEntity::getScore)));

        List<LocalDate> days = stats.stream()
                .map(EmployeeDailyStatsEntity::getStartDay)
                .distinct()
                .sorted(Comparator.reverseOrder()) // najnowsze z lewej
                .toList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Statystyki " + from + " do " + to);


        CellStyle percentStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        percentStyle.setDataFormat(format.getFormat("0.00%"));

        int rowIdx = 1;

        // Nagłówek
        Row header = sheet.createRow(0);
        for (int i = 0; i < days.size(); i++) {
            header.createCell(i + 1).setCellValue(days.get(i).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }
        header.createCell(days.size() + 1).setCellValue("Średnia dzienna pracownika");
        header.createCell(days.size() + 2).setCellValue("Średnia dla okresu");

        double overallTotal = 0;
        int totalRows = 0;

        for (Map.Entry<Integer, Map<LocalDate, Double>> entry : grouped.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            String employeeName = employeeNames.getOrDefault(entry.getKey(), "Nieznany");
            Cell employeeCell = row.createCell(0);
            employeeCell.setCellValue(employeeName);


            double employeeTotal = 0;
            int employeeCount = 0;

            for (int i = 0; i < days.size(); i++) {
                Double score = entry.getValue().get(days.get(i));
                Cell cell = row.createCell(i + 1);
                if (score != null) {
                    // Zamiana wartości na odpowiednią liczbę dziesiętną, np. 35% = 0.35
                    double percentageValue = score; // Wartość już jest dziesiętną (np. 0.35)
                    cell.setCellValue(percentageValue);
                    cell.setCellStyle(percentStyle); // Styl procentowy
                    employeeTotal += percentageValue;
                    employeeCount++;
                } else {
                    cell.setCellValue(""); // Puste komórki
                }
            }

            // Średnia dzienna dla pracownika (użycie formuły Excel)
            if (employeeCount > 0) {
                String averageFormula = String.format("AVERAGEIF(B%d:%s%d, \"<>\" )", rowIdx, (char) ('B' + days.size() - 1), rowIdx);
                Cell avgCell = row.createCell(days.size() + 1);
                avgCell.setCellFormula(averageFormula); // Formuła Excel
                avgCell.setCellStyle(percentStyle); // Styl procentowy
            }

            overallTotal += employeeTotal;
            totalRows += employeeCount;
        }

        // Obliczanie średniej z całej tabeli (użycie formuły Excel)
        Row totalRow = sheet.createRow(rowIdx);


        String overallAverageFormula = String.format("AVERAGEIF(B%d:%s%d, \"<>\" )", 2, (char) ('B' + days.size() - 1), rowIdx);
        totalRow.createCell(days.size() + 1).setCellFormula(overallAverageFormula); // Formuła Excel
        totalRow.createCell(days.size() + 2).setCellFormula(overallAverageFormula); // Formuła Excel

        // Formatowanie komórek (procenty)
        for (int i = 0; i < days.size() + 3; i++) {
            sheet.autoSizeColumn(i); // Automatyczne dopasowanie szerokości kolumn
        }

        // Zapisanie do pliku
        File file = Files.createTempFile("stats_", ".xlsx").toFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
            out.flush();  // zalecane
        }
        workbook.close();

        return file;
    }
}
