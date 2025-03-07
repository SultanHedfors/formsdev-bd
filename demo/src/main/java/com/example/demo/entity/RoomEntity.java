package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "STANOWISKO")
public class RoomEntity {

    @Id
    @Column(name = "STANOWISKO_ID")
    private Integer roomId;

    @Column(name = "STANOWISKO_NAZWA")
    private String roomName;

    @Column(name = "STANOWISKO_UWAGI", unique = true)
    private String roomCode;

    @ManyToMany
    @JoinTable(
            name = "stanzabieg",
            joinColumns = @JoinColumn(name = "stanowisko_id"),
            inverseJoinColumns = @JoinColumn(name = "zabieg_id")
    )
    private Set<ProcedureEntity> procedures = new HashSet<>();
}
