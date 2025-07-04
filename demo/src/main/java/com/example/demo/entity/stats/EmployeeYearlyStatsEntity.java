package com.example.demo.entity.stats;

import com.example.demo.entity.id_class.EmployeeYearKey;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "emp_year_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(EmployeeYearKey.class)
public class EmployeeYearlyStatsEntity {

    @Id
    @Column(name = "emp_id")
    private Integer employeeId;

    @Id
    @Column(name = "year_val")
    private Integer year;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
