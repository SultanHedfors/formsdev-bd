package com.example.demo.controller;


import com.example.demo.dto.bsn_logic_dto.ClinicDto;
import com.example.demo.service.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    @GetMapping
    public ResponseEntity<List<ClinicDto>> getAllClinics() {
        List<ClinicDto> clinics = clinicService.findAll();
        return ResponseEntity.ok(clinics);
    }

    @PostMapping
    public ResponseEntity<ClinicDto> createClinic(@RequestBody ClinicDto clinicDto) {
        ClinicDto saved = clinicService.save(clinicDto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicDto> updateClinic(@PathVariable Long id, @RequestBody ClinicDto clinicDto) {
        ClinicDto updated = clinicService.update(id, clinicDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClinic(@PathVariable Long id) {
        clinicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
