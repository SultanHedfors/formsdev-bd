package com.example.demo.controller;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.dto.bsn_logic_dto.ProcedureDto;
import com.example.demo.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<List<ProcedureDto>> getAllActivities() {
        List<ProcedureDto> procedures = activityService.findAll();
        return ResponseEntity.ok(procedures);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedureDto> getActivity(@PathVariable Long id) {
        ProcedureDto procedure = activityService.findById(id);
        return procedure != null ? ResponseEntity.ok(procedure)
                : ResponseEntity.notFound().build();
    }

    @PatchMapping
    public ResponseEntity<ActivityDto> markActivityAsOwn(@RequestBody ActivityDto activityDto) {
//        OwnProcedureDto saved = procedureService.save(ownProcedureDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<ProcedureDto> makeOwnerAsPerSchedule(@RequestBody ProcedureDto procedureDto) {
        ProcedureDto saved = activityService.save(procedureDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}