package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "STANOWISKO")
@ToString(exclude = {"activities", "procedures"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RoomEntity {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "STANOWISKO_ID")
    private Integer roomId;

    @Column(name = "STANOWISKO_NAZWA")
    @EqualsAndHashCode.Include
    private String roomName;

    @Column(name = "STANOWISKO_UWAGI", unique = true)
    @EqualsAndHashCode.Include
    private String roomCode;

    @OneToMany(mappedBy = "room")
    @Builder.Default
    private Set<ActivityEntity> activities = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "stanzabieg",
            joinColumns = @JoinColumn(name = "stanowisko_id"),
            inverseJoinColumns = @JoinColumn(name = "zabieg_id")
    )
    private Set<ProcedureEntity> procedures = new HashSet<>();
}

