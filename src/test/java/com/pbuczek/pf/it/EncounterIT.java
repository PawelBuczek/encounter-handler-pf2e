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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.pbuczek.pf.encounter.Encounter.MAX_DESCRIPTION_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
class EncounterIT extends _BaseIT {

    private final List<Integer> createdEncounterIds = new ArrayList<>();
    private static final String encName = "testEncounterName";

    @Autowired
    private EncounterRepository encounterRepo;

    @AfterEach
    void tearDown() {
        createdEncounterIds.forEach(id -> encounterRepo.deleteEncounter(id));
    }

    @Test
    @SneakyThrows
    void encounterIsCreatedCorrectly() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MvcResult result = getResultActionsForCreatingEncounter(userId, "test")
                .andExpect(status().isOk()).andReturn();

        Encounter createdEncounter = mapper.readValue(result.getResponse().getContentAsString(), Encounter.class);
        basicEncounterChecks(createdEncounter);

        assertAll("Verify Encounter properties",
                () -> assertThat(createdEncounter.getName()).isEqualTo(encName),
                () -> assertThat(createdEncounter.getUserId()).isEqualTo(userId),
                () -> assertThat(createdEncounter.getDescription()).isEqualTo("test"),
                () -> assertThat(createdEncounter.getPublished()).isFalse(),
                () -> assertThat(createdEncounter.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now()));

        Optional<Encounter> optionalEncounter = encounterRepo.findById(createdEncounter.getId());
        assertThat(optionalEncounter).isPresent();
        assertThat(optionalEncounter.get()).isEqualTo(createdEncounter);
    }

    @Test
    @SneakyThrows
    void cannotCreateEncounterWithDescriptionTooLong() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MockHttpServletResponse response = getResultActionsForCreatingEncounter(userId,
                RandomStringUtils.random(3001, true, true))
                .andExpect(status().isBadRequest()).andReturn().getResponse();

        assertThat(response.getErrorMessage()).isEqualTo(
                String.format("description too long. Max '%d' signs allowed.", MAX_DESCRIPTION_LENGTH));
    }

    @Test
    @SneakyThrows
    void differentStandardUserCannotReadEncounterThatIsNotPublished() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MvcResult postResult = getResultActionsForCreatingEncounter(userId, "test")
                .andExpect(status().isOk()).andReturn();

        Encounter createdEncounter = mapper.readValue(postResult.getResponse().getContentAsString(), Encounter.class);
        basicEncounterChecks(createdEncounter);
        Integer createdEncounterId = createdEncounter.getId();

        int secondUserId = createUser(TEST_USERNAME_STANDARD_2, TEST_EMAIL_STANDARD_2);
        enableUserAccount(secondUserId);

        assertThat(this.mockMvc.perform(
                        get("/encounter/" + createdEncounterId)
                                .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_STANDARD_2)))
                .andExpect(status().isForbidden()).andReturn().getResponse().getErrorMessage())
                .isEqualTo("not authorized for this resource");
    }

    @Test
    @SneakyThrows
    void differentStandardUserCanReadEncounterThatIsPublished() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MvcResult postResult = getResultActionsForCreatingEncounter(userId, "test")
                .andExpect(status().isOk()).andReturn();

        Encounter createdEncounter = mapper.readValue(postResult.getResponse().getContentAsString(), Encounter.class);
        basicEncounterChecks(createdEncounter);
        Integer createdEncounterId = createdEncounter.getId();

        int secondUserId = createUser(TEST_USERNAME_STANDARD_2, TEST_EMAIL_STANDARD_2);
        enableUserAccount(secondUserId);
        publishUnpublishEncounter(createdEncounterId);

        MockHttpServletResponse getResponse = this.mockMvc.perform(
                        get("/encounter/" + createdEncounterId)
                                .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_STANDARD_2)))
                .andExpect(status().isOk()).andReturn().getResponse();
        Encounter foundEncounter = mapper.readValue(getResponse.getContentAsString(), Encounter.class);

        assertThat(foundEncounter).usingRecursiveComparison()
                .ignoringFields("published")
                .isEqualTo(createdEncounter);
    }

    @SneakyThrows
    private ResultActions getResultActionsForCreatingEncounter(Integer userId, String description) {
        return this.mockMvc.perform(
                post("/encounter")
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ow.writeValueAsString(new EncounterDto(encName, userId, description))));
    }

    private void basicEncounterChecks(Encounter createdEncounter) {
        assert createdEncounter != null;
        assertThat(createdEncounter.getId()).isNotNull();
        createdEncounterIds.add(createdEncounter.getId());
    }

    private void publishUnpublishEncounter(int encounterId) throws Exception {
        this.mockMvc.perform(
                patch("/encounter/published/" + encounterId)
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1)));
    }

}