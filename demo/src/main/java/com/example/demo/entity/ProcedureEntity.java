package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ZABIEG")
public class ProcedureEntity {


    @Id
    @Column(name = "ZABIEG_ID")
    private Integer procedureId;

    @Column(name = "ZABIEG_PUNKTY")
    private Integer procedureActualTime;

    @ManyToMany(mappedBy = "procedures")
    private Set<RoomEntity> rooms = new HashSet<>();

    @OneToMany(mappedBy = "activityId")
    private List<ActivityEntity> activities;

    @Column(name = "ZABIEG_NAZWA")
    private String procedureName;

    @Column(name = "ZABIEG_NAZWA2")
    private String procedureType;

}
