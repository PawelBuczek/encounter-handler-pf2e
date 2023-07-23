package com.pbuczek.pf.encounter;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@ResponseBody
@Data
@NoArgsConstructor
@Entity
@Table(name = "encounter")
public class Encounter {

    public Encounter(String name, Integer userId, String description) {
        this.name = name;
        this.userId = userId;
        this.published = false;
        this.description = description;
        this.timeCreated = LocalDateTime.now(ZoneOffset.UTC);
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
    private Boolean published; //publicly available

    @Nonnull
    private String name;

    private String description;

    @Nonnull
    private LocalDateTime timeCreated;

}


