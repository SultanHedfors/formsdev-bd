package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "ZABIEG")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProcedureEntity {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "ZABIEG_ID")
    private Integer procedureId;

    @Column(name = "ZABIEG_PUNKTY")
    @EqualsAndHashCode.Include
    private Integer procedureActualTime;

    @OneToMany(mappedBy = "activityId")
    private List<ActivityEntity> activities;

    @Column(name = "ZABIEG_NAZWA")
    @EqualsAndHashCode.Include
    private String procedureName;

    @Column(name = "ZABIEG_NAZWA2")
    @EqualsAndHashCode.Include
    private String procedureType;

    @Column(name = "ZABIEG_UWAGI")
    @EqualsAndHashCode.Include
    private String workMode;
}

