package com.example.demo.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

import java.time.LocalDate;
import java.util.List;

public class ExcelUtil {

    //Preventing class instantiation
    private ExcelUtil() {
    }

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
            default -> throw new UnsupportedOperationException("Unsupported Excel cell type");

        }
    }

    public static List<Integer> getActualExcelIndexes(List<Integer> poiReadIndexes) {
        return poiReadIndexes.stream().map(i -> i + 1)
                .toList();
    }

    public static CellStyle formattedPercentStyle(Workbook workbook) {
        CellStyle percentStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        percentStyle.setDataFormat(format.getFormat("0.00%"));
        return percentStyle;
    }

    public static String averageFormula(List<LocalDate> statsDates, int rowIdx) {
        String startCol = CellReference.convertNumToColString(1);
        String endCol = CellReference.convertNumToColString(statsDates.size());
        return String.format("AVERAGEIF(%s%d:%s%d,\"<>\")", startCol, rowIdx + 1, endCol, rowIdx + 1);
    }

    public static String totalAverageFormula(List<LocalDate> statsDates, int rowIdx) {
        String startCol = CellReference.convertNumToColString(1);
        String endCol = CellReference.convertNumToColString(statsDates.size());
        return String.format("AVERAGEIF(%s2:%s%d,\"<>\")", startCol, endCol, rowIdx);
    }

    public static void setColumnSize(List<LocalDate> statsDates, Sheet sheet) {
        for (int i = 0; i < statsDates.size() + 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
