package com.example.demo.service;

import com.example.demo.exception.ScheduleValidationException;
import com.example.demo.schedule.processor.ScheduleReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileHandlerService {

    @Value("${upload.directory}")
    private String uploadDir;

    private final ScheduleReader scheduleReader;


    public void handleScheduleUpload(MultipartFile file, String authorizationHeader) throws IOException {
        if (file.isEmpty() || !Objects.requireNonNull(file.getOriginalFilename()).endsWith(".xlsx")) {
            throw new ScheduleValidationException("Nieprawidłowy format pliku. Wymagany .xlsx");
        }

        deleteAllFilesInDirectory(uploadDir);

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(uploadDir).resolve(fileName).toAbsolutePath().normalize();
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath);

        log.info("Plik został zapisany na dysku: {}", targetPath);

        scheduleReader.mapRowsToEntities(targetPath.toString(), authorizationHeader);
    }


    private void deleteAllFilesInDirectory(String directoryPath) throws IOException {
        Path dirPath = Paths.get(directoryPath);

        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try (Stream<Path> paths = Files.walk(dirPath)) {
                paths.filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                log.info("Usunięto plik: {}", path);
                            } catch (IOException e) {
                                log.error("Błąd podczas usuwania pliku: {}", path, e);
                            }
                        });
            }
        } else {
            log.warn("Katalog {} nie istnieje lub nie jest katalogiem.", directoryPath);
        }
    }

    public Resource getLatestTxtReport() throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(uploadDir))) {
            Path latestFile = paths
                    .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".txt"))
                    .max(Comparator.comparing(p -> {
                        try {
                            return Files.getLastModifiedTime(p);
                        } catch (IOException e) {
                            return FileTime.fromMillis(0);
                        }
                    }))
                    .orElseThrow(() -> new FileNotFoundException("Nie znaleziono pliku .txt"));

            Resource resource = new UrlResource(latestFile.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IOException("Plik jest niedostępny do odczytu");
            }
            return resource;
        }
    }
}
