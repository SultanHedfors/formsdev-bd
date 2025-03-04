package com.example.demo.controller;

import com.example.demo.dto.bsn_logic_dto.ProcedureDto;
import com.example.demo.service.ProcedureService;
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
public class ProcedureController {

    private final ProcedureService procedureService;

    @GetMapping
    public ResponseEntity<List<ProcedureDto>> getAllProcedures() {
        List<ProcedureDto> procedures = procedureService.findAll();
        return ResponseEntity.ok(procedures);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedureDto> getProcedure(@PathVariable Long id) {
        ProcedureDto procedure = procedureService.findById(id);
        return procedure != null ? ResponseEntity.ok(procedure)
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ProcedureDto> createProcedure(@RequestBody ProcedureDto procedureDto) {
        ProcedureDto saved = procedureService.save(procedureDto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcedureDto> updateProcedure(@PathVariable Long id, @RequestBody ProcedureDto procedureDto) {
        ProcedureDto updated = procedureService.update(id, procedureDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProcedure(@PathVariable Long id) {
        procedureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}