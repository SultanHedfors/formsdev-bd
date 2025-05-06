package com.example.demo.entity;

import com.example.demo.util.BooleanToSmallIntConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "work_schedule")
@Builder
public class WorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 7)
    private String yearMonth; // Stored as VARCHAR(7) in format "YYYY-MM"

    @Column(nullable = false)
    private Integer dayOfMonth;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private UserEntity employee; // Reference to Employee (employee_id)

    @ManyToOne
    @JoinColumn(name = "substitute_employee_id")
    private UserEntity substituteEmployee; // Reference to Substitute Employee

    @Column(length = 250)
    private String roomSymbol;

    @Column(nullable = false, length = 3)
    private String workMode; // Stored as VARCHAR(3) (U, UW, ZL, etc.)

    @Column(name = "work_start_time", length = 8)
    private String workStartTime; // Stored as VARCHAR(8) in format "HH:MM:SS"

    @Column(name = "work_end_time", length = 8)
    private String workEndTime; // Stored as VARCHAR(8) in format "HH:MM:SS"

    @Column
    private Integer workDurationMinutes;

    @Convert(converter = BooleanToSmallIntConverter.class)
    @Column(name = "processed")
    private Boolean processed;


    @ManyToOne
    @JoinColumn(name = "activity_id")
    private ActivityEntity activity;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");


}
