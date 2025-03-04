package com.example.demo.util;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class ScheduleReader {


    @Value("${schedule.filepath}")
    private String excelFilePath;

    public void loadExcelData() {
        try {
            File excelFile = getLatestExcelFile();
            if (excelFile == null) {
                System.out.println("No Excel file found in the specified directory.");
                return;
            }

            try (FileInputStream fis = new FileInputStream(excelFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0); // Load first sheet

                for (Row row : sheet) {
                    for (Cell cell : row) {
                        System.out.print(cell.toString() + "\t");
                    }
                    System.out.println();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

}
