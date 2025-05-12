package com.example.demo.controller;

import com.example.demo.dto.UploadResponseDto;
import com.example.demo.service.ScheduledActivityToWSService;
import com.example.demo.util.ScheduleReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    @Value("${upload.directory}")
    private String uploadDir;

    private final ScheduleReader scheduleReader;
    private final ScheduledActivityToWSService scheduledActivityToWSService;


    @PostMapping("/upload-schedule")
    public ResponseEntity<UploadResponseDto> uploadSchedule(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(new UploadResponseDto(false, "Nieprawidłowy format pliku. Wymagany .xlsx"));
        }

        try {
            deleteAllFilesInDirectory(uploadDir); // Funkcja do usuwania plików
        } catch (IOException e) {
            log.error("Błąd podczas usuwania plików z folderu uploads: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new UploadResponseDto(false, "Błąd przy czyszczeniu folderu uploads"));
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
            return ResponseEntity.internalServerError().body(new UploadResponseDto(false, e.getMessage()));
        }
    }


    // Endpoint do anulowania przetwarzania
    @PostMapping("/cancel-processing")
    public ResponseEntity<String> cancelProcessing() {
        try {
            // Anulowanie procesu w ScheduleReader
            scheduleReader.cancelProcessing();
            log.info(">>> Proces przetwarzania został anulowany w ScheduleReader.");

            // Anulowanie procesu w ScheduledActivityToWSService
            scheduledActivityToWSService.cancelProcessing();
            log.info(">>> Proces przetwarzania został anulowany w ScheduledActivityToWSService.");

            return ResponseEntity.ok("Przetwarzanie zostało anulowane.");

        } catch (Exception e) {
            log.error("Błąd podczas anulowania przetwarzania: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Błąd podczas anulowania przetwarzania.");
        }
    }


    // Funkcja do usuwania wszystkich plików w folderze
    private void deleteAllFilesInDirectory(String directoryPath) throws IOException {
        Path dirPath = Paths.get(directoryPath);
        try (Stream<Path> paths = Files.walk(dirPath)) {
            paths.filter(Files::isRegularFile)  // Filtruj tylko pliki
                    .forEach(path -> {
                        try {
                            Files.delete(path);  // Usuń plik
                            log.info("Usunięto plik: {}", path);
                        } catch (IOException e) {
                            log.error("Błąd podczas usuwania pliku: {}", path, e);
                        }
                    });
        }
    }

    @GetMapping("/download-latest-report")
    public ResponseEntity<Resource> downloadLatestTxtFile() {
        try (Stream<Path> paths = Files.list(Paths.get(uploadDir))) {
            // Znajdź najnowszy plik .txt w katalogu uploads
            Path latestFile = paths
                    .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".txt"))
                    .max((p1, p2) -> {
                        try {
                            FileTime t1 = Files.getLastModifiedTime(p1);
                            FileTime t2 = Files.getLastModifiedTime(p2);
                            return t1.compareTo(t2);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .orElse(null);

            if (latestFile == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(latestFile.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(500).body(null);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Błąd przy znajdowaniu lub ładowaniu pliku .txt: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}