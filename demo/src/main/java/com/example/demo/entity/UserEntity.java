package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "EMPLOYEE")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMPLOYEE_ID")
    private Long id;

    @Column(unique = true,name = "EMPLOYEE_USERNAME")
    private String username;

    @Column(name = "EMPLOYEE_PASSWORD")
    private String password;

    private LocalDateTime employeeCreationdate;

    private int employee_kod_systemu;

    private int zmianatyp_id;

    private int employee_is_admin;

    @PrePersist
    protected void onCreate() {
        this.employeeCreationdate = LocalDateTime.now();
        this.employee_kod_systemu = 1;
        this.zmianatyp_id = 4;
        this.employee_is_admin = 0;
    }

}
