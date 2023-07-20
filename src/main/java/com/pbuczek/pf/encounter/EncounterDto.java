package com.pbuczek.pf.encounter;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class EncounterDto {
    private String name;

    private Integer userId;
    private String description;
}
