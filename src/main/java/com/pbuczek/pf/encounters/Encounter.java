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

    public Encounter(String name, String description) {
        this.name = name;
        this.description= description;
    }

    public Encounter(EncounterDto encounterDto) {
        this(encounterDto.getName(), encounterDto.getDescription());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    private String name;

    private String description;
}


