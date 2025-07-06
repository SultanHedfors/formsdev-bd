package com.example.demo.controller;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.example.demo.util.AuthUtil.userFromSecurityContext;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<Page<ActivityDto>> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        String username = userFromSecurityContext();
        return ResponseEntity.ok(activityService.findAllActivities(page, size, username, startDate,
                endDate, month, sortDirection));
    }


    @PatchMapping
    public ResponseEntity<ActivityDto> markActivityAsOwn(@RequestBody ActivityDto activityDto) {
        String username = userFromSecurityContext();
        ActivityDto returnedDto = activityService.markActivityAsOwn(activityDto, username);
        return ResponseEntity.ok(returnedDto);
    }


    @PostMapping("/old")
    public ResponseEntity<ActivityDto> returnToOldAssignment(@RequestBody ActivityDto activityDto) {
        String username = userFromSecurityContext();
        ActivityDto returnedActivityDto = activityService.returnToOldAssignment(activityDto, username);
        return new ResponseEntity<>(returnedActivityDto, HttpStatus.CREATED);
    }


}