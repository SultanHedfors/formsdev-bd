package com.example.demo.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ExcelUtil {

    protected static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING -> {
                return cell.getStringCellValue().trim();
            }
            case NUMERIC -> {
                DataFormatter formatter = new DataFormatter();
                return formatter.formatCellValue(cell);
            }
            case BOOLEAN -> {
                return String.valueOf(cell.getBooleanCellValue());
            }
            case FORMULA -> {
                return cell.getCellFormula();
            }
            case BLANK -> {
                return "";
            }
            default -> {
                return "UNKNOWN";
            }
        }
    }

    @SuppressWarnings("all")
    protected static File getLatestExcelFile(String excelFilePath) {
        Path directoryPath = Paths.get(excelFilePath);

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

    protected static List<Integer> getActualExcelIndexes(List<Integer> poiReadIndexes){
        return poiReadIndexes.stream().map(i->i+1)
                .collect(Collectors.toList());
    }
}
