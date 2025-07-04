package com.example.demo.entity.stats;

import com.example.demo.entity.id_class.EmployeeMonthKey;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "emp_month_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(EmployeeMonthKey.class)
public class EmployeeMonthlyStatsEntity {

    @Id
    @Column(name = "emp_id")
    private Integer employeeId;

    @Id
    @Column(name = "month_val", length = 7)
    private String month;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
