package com.pbuczek.pf.encounter;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
class EncounterTest {

    @Test
    void encounterIsGeneratedCorrectly() {
        Encounter enc = new Encounter("enc", 1,
                RandomStringUtils.random(Encounter.MAX_DESCRIPTION_LENGTH, true, true));

        EncounterDto newEncDto = new EncounterDto();
        newEncDto.setName("enc");
        newEncDto.setUserId(1);
        newEncDto.setDescription(enc.getDescription());
        Encounter newEnc = new Encounter(newEncDto);

        assertAll("Verify encounters properties",
                () -> assertThat(enc.getId()).isEqualTo(newEnc.getId()).isNull(),
                () -> assertThat(enc.getUserId()).isEqualTo(newEnc.getUserId()).isEqualTo(1),
                () -> assertThat(enc.getPublished()).isEqualTo(newEnc.getPublished()).isFalse(),
                () -> assertThat(enc.getName()).isEqualTo(newEnc.getName()).isEqualTo("enc"),
                () -> assertThat(enc.getDescription()).isEqualTo(newEnc.getDescription()).hasSize(3000),
                () -> assertThat(enc.getTimeCreated())
                        .isBeforeOrEqualTo(newEnc.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now()));
    }

    @Test
    void encounterCannotBeGeneratedWithDescriptionOverLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                        new Encounter("enc",
                                1,
                                RandomStringUtils.random(Encounter.MAX_DESCRIPTION_LENGTH + 1, true, true)),
                String.format("Description is too long. Max length: %d", Encounter.MAX_DESCRIPTION_LENGTH));
    }

}