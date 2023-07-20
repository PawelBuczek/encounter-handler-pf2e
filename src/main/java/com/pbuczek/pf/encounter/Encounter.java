package com.pbuczek.pf.encounter;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@NoArgsConstructor
@Entity
@Table(name = "encounter")
public class Encounter {

    public Encounter(String name, Integer userId, String description) {
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.timeCreated = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Encounter(EncounterDto encounterDto) {
        this(encounterDto.getName(), encounterDto.getUserId(), encounterDto.getDescription());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    private Boolean published; //publicly available

    @Nonnull
    private Integer userId;

    @Nonnull
    private String name;

    private String description;

    @Nonnull
    private LocalDateTime timeCreated;

}


