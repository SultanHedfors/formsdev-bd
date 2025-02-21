package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ProcedureDto;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ProcedureService {

    public List<ProcedureDto> findAll() {
        // TODO: Implement service logic to retrieve all procedures
        return Collections.emptyList();
    }

    public ProcedureDto findById(Long id) {
        // TODO: Implement service logic to retrieve a procedure by id
        return null;
    }

    public ProcedureDto save(ProcedureDto procedureDto) {
        // TODO: Implement service logic to save a new procedure
        return procedureDto;
    }

    public ProcedureDto update(Long id, ProcedureDto procedureDto) {
        // TODO: Implement service logic to update a procedure by id
        return procedureDto;
    }

    public void delete(Long id) {
        // TODO: Implement service logic to delete a procedure by id
    }
}
