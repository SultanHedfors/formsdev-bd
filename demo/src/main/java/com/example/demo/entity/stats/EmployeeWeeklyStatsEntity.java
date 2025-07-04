package com.example.demo.entity.stats;

import com.example.demo.entity.id_class.EmployeeWeekKey;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emp_week_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(EmployeeWeekKey.class)
public class EmployeeWeeklyStatsEntity {

    @Id
    @Column(name = "emp_id")
    private Integer employeeId;

    @Id
    @Column(name = "week_start")
    private LocalDate weekStart;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
