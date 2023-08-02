package com.pbuczek.pf.encounter;

import com.pbuczek.pf.interfaces.JpaEntity;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Data
@Entity
@NoArgsConstructor
@Table(name = "encounter")
public class Encounter implements JpaEntity {

    public static final Integer MAX_DESCRIPTION_LENGTH = 3000;

    public Encounter(String name, Integer userId, String description) {
        this.name = name == null ? "" : name.trim();
        this.userId = userId;
        this.description = description == null ? "" : description.trim();
        this.timeCreated = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    public Encounter(EncounterDto encounterDto) {
        this(encounterDto.getName(), encounterDto.getUserId(), encounterDto.getDescription());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Nonnull
    private Integer userId;
    @Nonnull
    private Boolean published = false; //true means publicly available
    @Nonnull
    private String name;
    private String description;
    @Nonnull
    private LocalDateTime timeCreated;

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }
}


