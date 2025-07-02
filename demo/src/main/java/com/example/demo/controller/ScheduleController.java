package com.example.demo.controller;

import com.example.demo.dto.UploadResponseDto;
import com.example.demo.schedule.processor.ScheduleReader;
import com.example.demo.service.FileHandlerService;
import com.example.demo.service.ScheduledActivityToWSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduleReader scheduleReader;
    private final ScheduledActivityToWSService scheduledActivityToWSService;
    private final FileHandlerService fileHandlerService;


    @PostMapping("/upload-schedule")
    public ResponseEntity<UploadResponseDto> uploadSchedule(@RequestParam("file") MultipartFile file,
                                                            @RequestHeader("Authorization") String authorizationHeader) throws IOException {
        fileHandlerService.handleScheduleUpload(file, authorizationHeader);
        return ResponseEntity.ok(new UploadResponseDto(true, "Plik został zapisany i przetworzony poprawnie."));
    }


    @PostMapping("/cancel-processing")
    public ResponseEntity<String> cancelProcessing() {

        scheduleReader.cancelProcessing();
        log.info(">>> Schedule processing was canceled in schedule reader");

        scheduledActivityToWSService.cancelProcessing();
        log.info(">>> Schedule processing was canceled in ScheduledActivityToWSService.");

        return ResponseEntity.ok("Przetwarzanie zostało anulowane.");
    }

    @GetMapping("/download-latest-report")
    public ResponseEntity<Resource> downloadLatestTxtFile() throws IOException {
        Resource resource = fileHandlerService.getLatestTxtReport();

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}



