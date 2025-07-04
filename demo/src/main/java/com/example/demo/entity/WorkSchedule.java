package com.example.demo.entity;

import com.example.demo.util.BooleanToSmallIntConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "work_schedule")
@Builder
public class WorkSchedule {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false)
    private Integer dayOfMonth;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private UserEntity employee;

    @ManyToOne
    @JoinColumn(name = "substitute_employee_id")
    private UserEntity substituteEmployee;

    @Column(length = 250)
    private String roomSymbol;

    @Column(nullable = false, length = 3)
    private String workMode;

    @Column(name = "work_start_time", length = 8)
    private String workStartTime;

    @Column(name = "work_end_time", length = 8)
    private String workEndTime;

    @Column
    private Integer workDurationMinutes;

    @Convert(converter = BooleanToSmallIntConverter.class)
    @Column(name = "processed")
    private Boolean processed;


}
