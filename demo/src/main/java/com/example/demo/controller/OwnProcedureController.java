package com.example.demo.controller;

import com.example.demo.service.OwnProcedureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.demo.dto.bsn_logic_dto.OwnProcedureDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/own-procedures")
@RequiredArgsConstructor
public class OwnProcedureController {

    private final OwnProcedureService ownProcedureService;

    @GetMapping
    public ResponseEntity<List<OwnProcedureDto>> getAllOwnProcedures() {
        List<OwnProcedureDto> ownProcedures = ownProcedureService.findAll();
        return ResponseEntity.ok(ownProcedures);
    }

    @PostMapping
    public ResponseEntity<OwnProcedureDto> createOwnProcedure(@RequestBody OwnProcedureDto ownProcedureDto) {
        OwnProcedureDto saved = ownProcedureService.save(ownProcedureDto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwnProcedure(@PathVariable Long id) {
        ownProcedureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
