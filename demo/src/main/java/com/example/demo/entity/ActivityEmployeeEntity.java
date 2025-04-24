package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_employee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "activity_id")
    private ActivityEntity activity;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private UserEntity employee;

    @Column(name = "user_modified", nullable = false)
    private boolean userModified;

    @ManyToOne
    @JoinColumn(name = "work_schedule_id")
    private WorkSchedule workSchedule;
}
