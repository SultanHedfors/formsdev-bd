package com.example.demo.dto.bsn_logic_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnProcedureDto {
    private Long id;
    // Reference to Procedure (by its id)
    private Long procedureId;
    // Reference to Employee (by its id)
    private Long employeeId;
    // Maps to "czasZaznaczenia"
    private LocalDateTime markingTime;
}
