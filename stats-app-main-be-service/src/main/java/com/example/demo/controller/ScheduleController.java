package com.example.demo.controller;

import com.example.demo.dto.ScheduleUploadResponseDto;
import com.example.demo.schedule.processor.ScheduleReader;
import com.example.demo.service.FileHandlerService;
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
    private final FileHandlerService fileHandlerService;


    @PostMapping("/upload-schedule")
    public ResponseEntity<ScheduleUploadResponseDto> uploadSchedule(@RequestParam("file") MultipartFile file,
                                                                    @RequestHeader("Authorization") String authorizationHeader) throws IOException {
        fileHandlerService.handleScheduleUpload(file, authorizationHeader);
        return ResponseEntity.ok(new ScheduleUploadResponseDto(true, "The file was processed and saved successfully."));
    }


    @PostMapping("/cancel-processing")
    public ResponseEntity<String> cancelProcessing() {

        scheduleReader.cancelProcessing();
        log.info(">>> Schedule processing in ScheduleReader was cancelled.");

        return ResponseEntity.ok("Processing was cancelled.");
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



