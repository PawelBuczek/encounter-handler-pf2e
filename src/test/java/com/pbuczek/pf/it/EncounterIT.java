package com.pbuczek.pf.it;

import com.pbuczek.pf.encounter.Encounter;
import com.pbuczek.pf.encounter.EncounterDto;
import com.pbuczek.pf.encounter.EncounterRepository;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.pbuczek.pf.encounter.Encounter.MAX_DESCRIPTION_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
class EncounterIT extends _BaseIT {

    private final Set<Integer> createdEncounterIds = new HashSet<>();
    private static final String ENC_NAME = "testEncounterName";

    @Autowired
    private EncounterRepository encounterRepo;

    @AfterEach
    void tearDown() {
        createdEncounterIds.forEach(id -> encounterRepo.deleteEncounter(id));
        createdUserIds.forEach(id -> userRepo.deleteUser(id));
    }

    @Test
    void encounterIsCreatedCorrectly() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MockHttpServletResponse response = createEncounter(userId, "test", HttpStatus.OK);

        Encounter createdEncounter = getEncounterFromResponse(response);

        assertAll("Verify Encounter properties",
                () -> assertThat(createdEncounter.getName()).isEqualTo(ENC_NAME),
                () -> assertThat(createdEncounter.getUserId()).isEqualTo(userId),
                () -> assertThat(createdEncounter.getDescription()).isEqualTo("test"),
                () -> assertThat(createdEncounter.getPublished()).isFalse(),
                () -> assertThat(createdEncounter.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now()));

        Optional<Encounter> optionalEncounter = encounterRepo.findById(createdEncounter.getId());
        assertThat(optionalEncounter).isPresent();
        assertThat(optionalEncounter.get()).isEqualTo(createdEncounter);
    }

    @Test
    void cannotCreateEncounterWithDescriptionTooLong() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        String description = RandomStringUtils.random(3001, true, true);
        MockHttpServletResponse response = createEncounter(userId, description, HttpStatus.BAD_REQUEST);

        assertThat(response.getErrorMessage()).isEqualTo(
                String.format("description too long. Max '%d' signs allowed.", MAX_DESCRIPTION_LENGTH));
    }

    @Test
    void differentStandardUserCannotReadEncounterThatIsNotPublished() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MockHttpServletResponse response = createEncounter(userId, "test", HttpStatus.OK);

        Encounter createdEncounter = getEncounterFromResponse(response);
        Integer createdEncounterId = createdEncounter.getId();

        int secondUserId = createUser(TEST_USERNAME_STANDARD_2, TEST_EMAIL_STANDARD_2);
        enableUserAccount(secondUserId);

        assertThat(getResponseForGetEncounterRequest(createdEncounterId, TEST_USERNAME_STANDARD_2, HttpStatus.FORBIDDEN)
                .getErrorMessage()).isEqualTo("not authorized for this resource");
    }

    @Test
    void differentStandardUserCanReadEncounterThatIsPublished() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MockHttpServletResponse postResponse = createEncounter(userId, "test", HttpStatus.OK);

        Encounter createdEncounter = getEncounterFromResponse(postResponse);
        Integer createdEncounterId = createdEncounter.getId();

        int secondUserId = createUser(TEST_USERNAME_STANDARD_2, TEST_EMAIL_STANDARD_2);
        enableUserAccount(secondUserId);
        publishUnpublishEncounter(createdEncounterId);

        MockHttpServletResponse getResponse =
                getResponseForGetEncounterRequest(createdEncounterId, TEST_USERNAME_STANDARD_2, HttpStatus.OK);
        Encounter foundEncounter = getEncounterFromResponse(getResponse);

        assertThat(foundEncounter).usingRecursiveComparison()
                .ignoringFields("published")
                .isEqualTo(createdEncounter);
    }

    @Test
    @SneakyThrows
    void adminCanReadEncounterThatIsNotPublished() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MockHttpServletResponse response = createEncounter(userId, "test", HttpStatus.OK);

        Encounter createdEncounter = getEncounterFromResponse(response);
        Integer createdEncounterId = createdEncounter.getId();

        MockHttpServletResponse getResponse =
                getResponseForGetEncounterRequest(createdEncounterId, TEST_USERNAME_ADMIN_1, HttpStatus.OK);
        Encounter foundEncounter = getEncounterFromResponse(getResponse);

        assertThat(foundEncounter).isEqualTo(createdEncounter);
    }

    @SneakyThrows
    private MockHttpServletResponse createEncounter(
            Integer userId, String description, HttpStatus expectedStatus) {

        return this.mockMvc.perform(post("/encounter")
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ow.writeValueAsString(new EncounterDto(ENC_NAME, userId, description))))
                .andExpect(status().is(expectedStatus.value())).andReturn().getResponse();
    }

    @SneakyThrows
    private MockHttpServletResponse getResponseForGetEncounterRequest(
            Integer encounterId, String username, HttpStatus expectedStatus) {

        return this.mockMvc.perform(get("/encounter/" + encounterId)
                        .header("Authorization", getBasicAuthenticationHeader(username)))
                .andExpect(status().is(expectedStatus.value())).andReturn().getResponse();
    }

    @SneakyThrows
    private void publishUnpublishEncounter(int encounterId) {
        this.mockMvc.perform(patch("/encounter/published/" + encounterId)
                .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1)));
    }

    @SneakyThrows
    private Encounter getEncounterFromResponse(MockHttpServletResponse response) {
        return getObjectFromResponse(response, Encounter.class, createdEncounterIds);
    }

}