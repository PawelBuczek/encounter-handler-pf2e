package com.pbuczek.pf.encounter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EncounterDto {
    private String name;
    private Integer userId;
    private String description;
}
