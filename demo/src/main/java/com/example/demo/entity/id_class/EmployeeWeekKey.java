package com.example.demo.entity.id_class;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWeekKey implements Serializable {

    private Integer employeeId;
    private LocalDate weekStart;

}
