package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private UserEntity employee;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
}
