package com.example.demo.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.util.List;

public class ExcelUtil {

    //Preventing class instantiation
    private ExcelUtil() {}

    public static String getCellValueAsString(Cell cell) {
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


    public static List<Integer> getActualExcelIndexes(List<Integer> poiReadIndexes) {
        return poiReadIndexes.stream().map(i -> i + 1)
                .toList();
    }
}
