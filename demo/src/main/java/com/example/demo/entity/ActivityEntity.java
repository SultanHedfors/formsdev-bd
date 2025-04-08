package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ZAJECIE")
public class ActivityEntity {

    @Id
    @Column(name = "ZAJECIE_ID")
    private Integer activityId;

    @Column(name = "ZAJECIE_DATA")
    private Timestamp activityDate;

    @Column(name = "ZAJECIE_GODZ")
    private Timestamp activityTime;

    @Column(name = "ZAJECIE_DATA_DODANIA")
    private Timestamp recordAddedTime;

    @ManyToOne
    @JoinColumn(name = "zabieg_id", referencedColumnName = "zabieg_id")
    private ProcedureEntity procedure;

    @ManyToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    private UserEntity employee;

    @ManyToOne
    @JoinColumn(name = "stanowisko_id", referencedColumnName = "STANOWISKO_ID")
    private RoomEntity room;

    @ManyToMany
    @JoinTable(
            name = "activity_employee",
            joinColumns = @JoinColumn(name = "activity_id", referencedColumnName = "ZAJECIE_ID"),
            inverseJoinColumns = @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    )
    private Set<UserEntity> employees = new HashSet<>();
}
