package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ZAJECIE")
@ToString(exclude = {"procedure", "employee", "room", "activityEmployees"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ActivityEntity {

    @Id
    @Column(name = "ZAJECIE_ID")
    @EqualsAndHashCode.Include
    private Integer activityId;

    @Column(name = "ZAJECIE_DATA")
    private LocalDateTime activityDate;

    @Column(name = "ZAJECIE_GODZ")
    private Timestamp activityTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zabieg_id", referencedColumnName = "zabieg_id")
    private ProcedureEntity procedure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    private UserEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stanowisko_id", referencedColumnName = "STANOWISKO_ID")
    private RoomEntity room;

    @ManyToMany
    @JoinTable(
            name = "activity_employee",
            joinColumns = @JoinColumn(name = "activity_id", referencedColumnName = "ZAJECIE_ID"),
            inverseJoinColumns = @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    )
    private Set<UserEntity> activityEmployees = new HashSet<>();
}
