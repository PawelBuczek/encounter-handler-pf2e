package com.pbuczek.pf.encounters;

import com.pbuczek.pf.encounters.dto.EncounterDto;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "encounter")
public class Encounter {

    public Encounter(String name, Integer userId, String description) {
        this.name = name;
        this.description = description;
        this.userId = userId;
    }

    public Encounter(EncounterDto encounterDto) {
        this(encounterDto.getName(), encounterDto.getUserId(), encounterDto.getDescription());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    private Integer userId; //was created by

    @Nonnull
    private String name;

    private String description;
}


