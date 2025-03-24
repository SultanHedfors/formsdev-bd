package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

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

    @Column(name= "ZAJECIE_DATA_DODANIA")
    private Timestamp recordAddedTime;

    @ManyToOne()
    @JoinColumn(name = "zabieg_id", referencedColumnName = "zabieg_id")
    private ProcedureEntity procedure;

    @ManyToOne()
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    private UserEntity employee;

}
