package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ZABIEG")
public class ProcedureEntity implements Serializable {

    @Id
    @Column(name = "ZABIEG_ID")
    private Integer procedureId;

    @Column(name = "ZABIEG_PUNKTY")
    private Integer procedureActualTime;

    @OneToMany(mappedBy = "activityId")
    private transient List<ActivityEntity> activities;

    @Column(name = "ZABIEG_NAZWA")
    private String procedureName;

    @Column(name = "ZABIEG_NAZWA2")
    private String procedureType;

    @Column(name = "ZABIEG_UWAGI")
    private String workMode;

}
