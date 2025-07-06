package com.example.demo.service;

import com.example.demo.exception.ScheduleValidationException;
import com.example.demo.schedule.processor.ScheduleReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileHandlerServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    ScheduleReader scheduleReader;

    FileHandlerService fileHandlerService;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileHandlerService = spy(new FileHandlerService(scheduleReader));
        doReturn(tempDir.toString()).when(fileHandlerService).getUploadDir();
    }

    @Test
    void handleScheduleUpload_shouldThrowForEmptyFile() {
        // arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        // act & assert
        assertThrows(ScheduleValidationException.class,
                () -> fileHandlerService.handleScheduleUpload(file, "Bearer xyz"));
    }

    @Test
    void handleScheduleUpload_shouldThrowForNonXlsx() {
        // arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "abc".getBytes());
        // act & assert
        assertThrows(ScheduleValidationException.class,
                () -> fileHandlerService.handleScheduleUpload(file, "Bearer xyz"));
    }

    @Test
    void handleScheduleUpload_shouldSaveFileAndCallScheduleReader() throws Exception {
        // arrange
        MockMultipartFile file = new MockMultipartFile("file", "schedule.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "testcontent".getBytes());

        // act
        fileHandlerService.handleScheduleUpload(file, "authHeader");

        // assert
        try (Stream<Path> files = Files.list(tempDir)) {
            assertTrue(files.anyMatch(path -> path.toString().endsWith(".xlsx")));
        }
        verify(scheduleReader).mapRowsToEntities(anyString(), eq("authHeader"));
    }

    @Test
    void deleteAllFilesInDirectory_shouldDeleteFiles() throws Exception {
        // arrange
        Path testFile = Files.createFile(tempDir.resolve("file1.txt"));
        assertTrue(Files.exists(testFile));

        // act
        fileHandlerService.handleScheduleUpload(
                new MockMultipartFile("file", "schedule.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "testcontent".getBytes()),
                "auth"
        );

        // assert
        try (Stream<Path> files = Files.list(tempDir)) {
            assertTrue(files.anyMatch(path -> path.toString().endsWith(".xlsx")));
        }
        assertFalse(Files.exists(testFile));
    }

    @Test
    void getLatestTxtReport_shouldThrowIfNoFile() {
        // act & assert
        assertThrows(FileNotFoundException.class, () -> fileHandlerService.getLatestTxtReport());
    }

    @Test
    void getLatestTxtReport_shouldReturnResource() throws Exception {
        // arrange
        Path file1 = Files.writeString(tempDir.resolve("a.txt"), "fileA");
        Path file2 = Files.writeString(tempDir.resolve("b.txt"), "fileB");

        Files.setLastModifiedTime(file1, FileTime.fromMillis(1000));
        Files.setLastModifiedTime(file2, FileTime.fromMillis(2000));
        // act
        Resource resource = fileHandlerService.getLatestTxtReport();
        // assert
        assertEquals(file2.toUri(), resource.getURI());
    }
}
