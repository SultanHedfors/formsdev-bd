package com.example.demo.controller;

import com.example.demo.dto.UploadResponseDto;
import com.example.demo.util.ScheduleReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    @Value("${upload.directory}")
    private String uploadDir;

    private final ScheduleReader scheduleReader;


    @PostMapping("/upload-schedule")
    public ResponseEntity<UploadResponseDto> uploadSchedule(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(new UploadResponseDto(false, "Nieprawidłowy format pliku. Wymagany .xlsx"));
        }

        try {
            // Zapisz plik na serwerze
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetPath = Paths.get(uploadDir).resolve(fileName).toAbsolutePath().normalize();
            Files.createDirectories(targetPath.getParent()); // upewnij się, że katalog istnieje
            Files.copy(file.getInputStream(), targetPath);

            log.info("Plik został zapisany na dysku: {}", targetPath);

            // Uruchom przetwarzanie na podstawie pełnej ścieżki pliku
            scheduleReader.mapRowsToEntities(targetPath.toString());

            return ResponseEntity.ok(new UploadResponseDto(true, "Plik został zapisany i przetworzony poprawnie."));

        } catch (Exception e) {
            log.error("Błąd podczas zapisywania lub przetwarzania pliku: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new UploadResponseDto(false, "Błąd przetwarzania: " + e.getMessage()));
        }
    }
}
