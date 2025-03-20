package com.example.demo.controller;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.service.OwnProcedureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/own-procedures")
@RequiredArgsConstructor
@Slf4j
public class OwnProcedureController {

    private final OwnProcedureService ownProcedureService;

    @GetMapping
    public ResponseEntity<List<ActivityDto>> getAllOwnProcedures() {
        List<ActivityDto> ownProcedures = ownProcedureService.findAll();
        return ResponseEntity.ok(ownProcedures);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwnProcedure(@PathVariable Long id) {
        ownProcedureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
