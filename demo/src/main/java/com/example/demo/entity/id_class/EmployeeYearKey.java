package com.example.demo.entity.id_class;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeYearKey implements Serializable {

    private Integer employeeId;
    private Integer year;

}
