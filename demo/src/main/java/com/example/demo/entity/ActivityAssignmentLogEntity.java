package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "activity_assignment_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityAssignmentLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private ActivityEntity activity;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "act_assign_log_emp",
            joinColumns = @JoinColumn(name = "activity_assignment_log_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private List<UserEntity> employees;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
}
