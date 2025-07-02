package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ZAJECIE")
@ToString(exclude = {"procedure", "employee", "room", "employees"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ActivityEntity implements Serializable {

    @Id
    @Column(name = "ZAJECIE_ID")
    @EqualsAndHashCode.Include
    private Integer activityId;

    @Column(name = "ZAJECIE_DATA")
    private Timestamp activityDate;

    @Column(name = "ZAJECIE_GODZ")
    private Timestamp activityTime;

    @Column(name = "ZAJECIE_DATA_DODANIA")
    private Timestamp recordAddedTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zabieg_id", referencedColumnName = "zabieg_id")
    private transient ProcedureEntity procedure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    private transient UserEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
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
