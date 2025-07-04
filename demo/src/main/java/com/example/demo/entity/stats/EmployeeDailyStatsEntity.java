package com.example.demo.entity.stats;

import com.example.demo.entity.id_class.EmployeeDateKey;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emp_day_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(EmployeeDateKey.class)
public class EmployeeDailyStatsEntity {

    @Id
    @Column(name = "emp_id")
    private Integer employeeId;

    @Id
    @Column(name = "stat_day")
    private LocalDate startDay;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
