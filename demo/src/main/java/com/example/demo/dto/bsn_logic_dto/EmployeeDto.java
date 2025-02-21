package com.example.demo.dto.bsn_logic_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private Long id;
    // Maps to "EMPLOYEE_FULLNAME"
    private String fullName;
    // Maps to "EMPLOYEE_KODKASJERA"
    private String employeeCode;
    // Maps to "EMPLOYEE_POSCE" (plain text – consider encryption in production)
    private String password;
    // Maps to "EMPLOYEE_LOYALTY_PIN"
    private Boolean loyaltyPin;
}
