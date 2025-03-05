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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
public class ScheduleReader {


    @Value("${schedule.filepath}")
    private String excelFilePath;

    private UserRepository userRepository;
    private ScheduleRepository scheduleRepository;
    public ScheduleReader(UserRepository userRepository,ScheduleRepository scheduleRepository) {
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
    }
@PostConstruct
    public List<WorkSchedule> mapRowsToEntities() {
        List<WorkSchedule> workSchedules = new ArrayList<>();
        try {
            File excelFile = getLatestExcelFile();
            if (excelFile == null) {
                System.out.println("No Excel file found in the specified directory.");
                return null;
            }

            try (FileInputStream fis = new FileInputStream(excelFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0); // Load first sheet
                YearMonth yearMonth = YearMonth.of(2025, 1); // Hardcoded January 2025

                // Read the relevant rows
                Row employeeRow = sheet.getRow(3);  // Employee name row
                Row workModeRow = sheet.getRow(3);  // Work mode row (F for working days)
                Row startTimeRow = sheet.getRow(4); // Start time row
                Row endTimeRow = sheet.getRow(5);   // End time row

                System.out.println(employeeRow.getCell(0));
                String employeeName = getCellValueAsString(employeeRow.getCell(0)).trim();

                // Iterate over the days of the month
                IntStream.range(1, 32).forEach(day -> {
                    Cell modeCell = workModeRow.getCell(day);
                    Cell startCell = startTimeRow.getCell(day);
                    Cell endCell = endTimeRow.getCell(day);

                    // Check if employee worked that day
                    System.out.println("mode val "+getCellValueAsString(modeCell));
                    System.out.println("start val "+getCellValueAsString(startCell));
                    System.out.println("end val "+getCellValueAsString(endCell));

                    if (modeCell != null && "F".equals(getCellValueAsString(modeCell).trim())) {
                        String startTime = (startCell != null) ? getCellValueAsString(startCell).trim() : null;
                        String endTime = (endCell != null) ? getCellValueAsString(endCell).trim() : null;

                        WorkSchedule schedule = WorkSchedule.builder()
                                .yearMonth(yearMonth.toString())
                                .dayOfMonth(day)
                                .employee(findEmployeeByCode(employeeName))
                                .workMode("F")
                                .workStartTime(startTime)
                                .workEndTime(endTime)
                                .workDurationMinutes(calculateDuration(startTime, endTime))
                                .build();

                        workSchedules.add(schedule);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch(Exception e){

        }
    System.out.println(workSchedules);

    scheduleRepository.saveAll(workSchedules);

        return workSchedules;}

    private File getLatestExcelFile() {
        Path directoryPath = Paths.get(excelFilePath);
        if (directoryPath == null) return null;

        try {
            return Files.list(directoryPath)
                    .filter(path -> path.toString().endsWith(".xlsx"))
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private static Integer calculateDuration(String startTime, String endTime) {
        if (startTime == null || endTime == null) return null;
        String[] startParts = startTime.split(":");
        String[] endParts = endTime.split(":");

        int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
        int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);

        return endMinutes - startMinutes;
    }

    private UserEntity findEmployeeByCode(String employeeCode){

        System.out.println("EMPLOYEE CODE: "+ employeeCode.toUpperCase());
        return userRepository.findByEmployeeCode(employeeCode.toUpperCase()).orElseThrow();
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Jeśli komórka powinna być tekstem, ale POI odczytuje ją jako NUMERIC
                DataFormatter formatter = new DataFormatter();
                return formatter.formatCellValue(cell); // Zwraca formatowaną wartość jako String
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "UNKNOWN";
        }}


}
