package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "STANOWISKO")
@ToString(exclude = {"activities", "procedures"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RoomEntity {

    @Id
    @Column(name = "STANOWISKO_ID")
    @EqualsAndHashCode.Include
    private Integer roomId;

    @Column(name = "STANOWISKO_NAZWA")
    private String roomName;

    @Column(name = "STANOWISKO_UWAGI", unique = true)
    private String roomCode;

    @OneToMany(mappedBy = "room")
    private Set<ActivityEntity> activities = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "stanzabieg",
            joinColumns = @JoinColumn(name = "stanowisko_id"),
            inverseJoinColumns = @JoinColumn(name = "zabieg_id")
    )
    private Set<ProcedureEntity> procedures = new HashSet<>();
}
