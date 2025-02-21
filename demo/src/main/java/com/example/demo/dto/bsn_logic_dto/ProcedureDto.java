package com.example.demo.dto.bsn_logic_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureDto {
    private Long id;
    // Maps to "dataRozpoczecia"
    private LocalDateTime startDateTime;
    // Maps to "planowanyCzas"
    private Integer plannedDuration;
    // Maps to "ZABIEG_PUNKTY"
    private Integer actualTime;
    // Maps to "ZABIEG_UWAGI" (contains clinic symbol & type info)
    private String clinicInfo;
}
