package com.example.demo.schedule.processor;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.example.demo.schedule.processor.ExcelValidateUtilTest.ExcelSheetValidations.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExcelValidateUtilTest {

    @Mock
    private LogUtil logUtil;

    @InjectMocks
    ExcelValidateUtil excelValidateUtil;

    @BeforeEach
    void prepareExcelValidate() {
        excelValidateUtil = new ExcelValidateUtil(logUtil);
    }

    @Test
    void shouldHandleValidationErrors() {
        //arrange
        excelValidateUtil.addValidationError("test error");
        var list = new ArrayList<String>();
        list.add("test error");
        when(logUtil.getLogMessages()).thenReturn(list);

        //act
        var ex = assertThrows(RuntimeException.class,
                () -> excelValidateUtil.handleValidationErrors("path", "name"));
        //assert
        assertEquals("Validation errors in uploaded file." + List.of("test error"), ex.getMessage());
        assertTrue(list.contains("test error"));

    }

    @Test
    void iValidDayShouldReturnFalseForNonExistingDayOfMonth() {
        //act & assert
        assertFalse(excelValidateUtil.isValidDay(YearMonth.of(2023, 4), 31));
    }

    @Test
    void iValidDayShouldReturnTrueForExistingDayOfMonth() {
        //act & assert
        assertTrue(excelValidateUtil.isValidDay(YearMonth.of(2023, 4), 30));
    }

    @Nested
    public class ExcelSheetValidations {
        XSSFWorkbook okWorkbook;
        XSSFWorkbook nonExistingEmployeeWorkbook;
        XSSFWorkbook missingHeaderWorkbook;

        private static final List<String> employeesCodes1 = List.of(
                "197", "AB", "201", "219", "221", "202", "191", "195", "203", "214", "223", "220",
                "EWR", "227", "222", "231", "171", "192", "DPI", "187", "188", "212", "KRA", "190",
                "206", "RKO", "205", "204", "203", "WMO", "209", "229", "228"
        );

        private static final List<String> roomCodes1 = List.of(
                "GAB1", "GAB2", "GAB3", "GAB4", "GAB5", "GAB6", "GAB7", "GAB8", "GAB9",
                "GABINET MASAŻU 7", "GABINET MASAŻU 8", "GABINET MASAŻU 9",
                "GABINET MASAŻU 10", "GABINET MASAŻU 1", "GABINET MASAŻU 6",
                "MASAŻ KLASYCZNY E"
        );

//        private static final Set<Integer> excpectedEmployeeRowIndecies = Set.of(
//                3, 6, 9, 12, 15, 18, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 58, 62, 66, 70, 74, 78, 82, 86, 90, 94, 98, 102, 106, 110, 114, 118
//        );

        enum TestData {
            OK, NON_EXISTING_EMPLOYEE, MISSING_HEADER
        }

        @BeforeEach
        void prepareDataForExcelValidations() throws IOException {
            okWorkbook = new XSSFWorkbook(new FileInputStream("src/test/excels/grafik_pracy_2025-01.xlsx"));
            nonExistingEmployeeWorkbook = new XSSFWorkbook(new FileInputStream("src/test/excels/grafik nieistniejacy employee  01-2025.xlsx"));
            missingHeaderWorkbook = new XSSFWorkbook(new FileInputStream("src/test/excels/grafik nie istniejacy header  01-2025.xlsx"));
        }

        @AfterEach
        void cleanup() throws IOException {
            if (okWorkbook != null) okWorkbook.close();
            if (nonExistingEmployeeWorkbook != null) nonExistingEmployeeWorkbook.close();
            if (missingHeaderWorkbook != null) missingHeaderWorkbook.close();
        }

        @ParameterizedTest
        @MethodSource("argumentsForValidateFirstColumnEntries")
        void validateFirstColumnEntriesTest(Sheet sheet, List<String> employeesCodes, List<String> roomCodes, TestData testData, String errorMessage) {
            Exception exMissingHeader;
            Exception exNonExistingEmployee;
            Executable validateMethodCall = () -> excelValidateUtil.validateFirstColumnEntries(sheet, employeesCodes, roomCodes);
            switch (testData) {
                case OK ->
                        assertTrue(() -> excelValidateUtil.validateFirstColumnEntries(sheet, employeesCodes, roomCodes));
                case MISSING_HEADER -> {
                    exMissingHeader = assertThrows(IllegalStateException.class, validateMethodCall);
                    assertEquals(errorMessage, exMissingHeader.getMessage());
                }
                case NON_EXISTING_EMPLOYEE -> {
                    exNonExistingEmployee = assertThrows(IllegalStateException.class, validateMethodCall);
                    assertEquals(errorMessage, exNonExistingEmployee.getMessage());
                    assertTrue(excelValidateUtil.getValidationErrors().contains(errorMessage));
                }
            }
        }


        //do ustawienia
        static Stream<Arguments> argumentsForValidateFirstColumnEntries() {
            return Stream.of(
                    Arguments.of(
                            getSheetFromWorkbook("src/test/excels/grafik_pracy_2025-01.xlsx"),
                            employeesCodes1, roomCodes1, OK, "no error expected"),
                    Arguments.of(
                            getSheetFromWorkbook("src/test/excels/grafik nieistniejacy employee  01-2025.xlsx"),
                            employeesCodes1, roomCodes1, NON_EXISTING_EMPLOYEE,
                            "❌ Invalid entry in first column at row 4: 'test' is not an employee code or a room name."),
                    Arguments.of(
                            getSheetFromWorkbook("src/test/excels/grafik nie istniejacy header  01-2025.xlsx"),
                            employeesCodes1, roomCodes1, MISSING_HEADER,
                            "❌ Header 'Kod pracownika' not found in the first column.")
            );
        }

        static Sheet getSheetFromWorkbook(String filePath) {
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filePath));
                return workbook.getSheetAt(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
