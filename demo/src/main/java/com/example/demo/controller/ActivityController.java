package com.example.demo.controller;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.dto.bsn_logic_dto.ProcedureDto;
import com.example.demo.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<Page<ActivityDto>> getAllActivities(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(activityService.findAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedureDto> getActivity(@PathVariable Long id) {
        ProcedureDto procedure = activityService.findById(id);
        return procedure != null ? ResponseEntity.ok(procedure)
                : ResponseEntity.notFound().build();
    }

    @PatchMapping
    public ResponseEntity<ActivityDto> markActivityAsOwn(@RequestBody ActivityDto activityDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            activityService.markActivityAsOwn(activityDto, username);
        } else {
            log.error("Could not retrieve user to assign an activity");
            return ResponseEntity.badRequest().body(activityDto);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ProcedureDto> makeOwnerAsPerSchedule(@RequestBody ProcedureDto procedureDto) {
        ProcedureDto saved = activityService.save(procedureDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}