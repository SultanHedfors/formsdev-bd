package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ClinicDto;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ClinicService {

    public List<ClinicDto> findAll() {
        // TODO: Implement service logic to retrieve all clinics
        return Collections.emptyList();
    }

    public ClinicDto save(ClinicDto clinicDto) {
        // TODO: Implement service logic to save a new clinic
        return clinicDto;
    }

    public ClinicDto update(Long id, ClinicDto clinicDto) {
        // TODO: Implement service logic to update a clinic by id
        return clinicDto;
    }

    public void delete(Long id) {
        // TODO: Implement service logic to delete a clinic by id
    }
}
