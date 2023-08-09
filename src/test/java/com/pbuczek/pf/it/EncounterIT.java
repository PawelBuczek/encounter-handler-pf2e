package com.pbuczek.pf.it;

import com.pbuczek.pf.encounter.Encounter;
import com.pbuczek.pf.encounter.EncounterDto;
import com.pbuczek.pf.encounter.EncounterRepository;
import com.pbuczek.pf.user.User;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.pbuczek.pf.encounter.Encounter.MAX_DESCRIPTION_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

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
        int createdEncounterId = createEncounter(userId, "").getId();

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
    @Test
    void encounterCanBeDeleted() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        enableUserAccount(userId);

        int createdEncounterId = createEncounter(userId, "").getId();

        assertThat(sendRequest(HttpMethod.DELETE,
                HttpStatus.OK, TEST_USERNAME_STANDARD_1, "/encounter/" + createdEncounterId, "")
                .getContentAsString()).isEqualTo("1");

        assertThat(encounterRepo.findById(createdEncounterId)).isEmpty();
    }

    @Test
    void standardUserCannotReadAllEncounters() {
        Integer userId = readObjectFromResponse(
                createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK), User.class).getId();
        enableUserAccount(userId);

        createEncounter(userId, "test", HttpStatus.OK);

        sendRequest(HttpMethod.GET, HttpStatus.FORBIDDEN, TEST_USERNAME_STANDARD_1, "/encounter", "");
    }

    @Test
    void adminCanReadAllEncounters() {
        Integer userId = readObjectFromResponse(
                createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK), User.class).getId();
        enableUserAccount(userId);

        Encounter createdEncounter1 = getEncounterFromResponse(createEncounter(userId, "test", HttpStatus.OK));
        Encounter createdEncounter2 = getEncounterFromResponse(createEncounter(userId, "test", HttpStatus.OK));

        MockHttpServletResponse response = sendRequest(HttpMethod.GET, HttpStatus.OK, TEST_USERNAME_ADMIN_1, "/encounter", "");

        List<Encounter> listOfEncounters = readListFromResponse(response, Encounter.class);

        assertThat(listOfEncounters).contains(createdEncounter1, createdEncounter2);
    }

    @Test
    void encountersCanBeFoundByUserId() {
        Integer userId = readObjectFromResponse(
                createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK), User.class).getId();
        enableUserAccount(userId);

        Encounter createdEncounter1 = getEncounterFromResponse(createEncounter(userId, "test", HttpStatus.OK));
        Encounter createdEncounter2 = getEncounterFromResponse(createEncounter(userId, "test", HttpStatus.OK));

        MockHttpServletResponse response = sendRequest(HttpMethod.GET, HttpStatus.OK, TEST_USERNAME_STANDARD_1,
                "/encounter/by-userid/" + userId, "");

        List<Encounter> listOfEncounters = readListFromResponse(response, Encounter.class);

        assertThat(listOfEncounters).containsExactlyInAnyOrder(createdEncounter1, createdEncounter2);
    }

    @Test
    void encountersCanBeFoundByUsername() {
        Integer userId = readObjectFromResponse(
                createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK), User.class).getId();
        enableUserAccount(userId);

        Encounter createdEncounter1 = getEncounterFromResponse(createEncounter(userId, "test", HttpStatus.OK));
        Encounter createdEncounter2 = getEncounterFromResponse(createEncounter(userId, "test", HttpStatus.OK));

        MockHttpServletResponse response = sendRequest(HttpMethod.GET, HttpStatus.OK, TEST_USERNAME_STANDARD_1,
                "/encounter/by-username/" + TEST_USERNAME_STANDARD_1, "");

        List<Encounter> listOfEncounters = readListFromResponse(response, Encounter.class);

        assertThat(listOfEncounters).containsExactlyInAnyOrder(createdEncounter1, createdEncounter2);
    }

    @Test
    void encounterDescriptionCanBeUpdated() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        enableUserAccount(userId);
        MockHttpServletResponse response = createEncounter(userId, "test", HttpStatus.OK);

        Encounter createdEncounter = getEncounterFromResponse(response);
        Integer createdEncounterId = createdEncounter.getId();

        response = sendRequest(HttpMethod.PATCH, HttpStatus.OK, TEST_USERNAME_STANDARD_1,
                "/encounter/description/" + createdEncounterId, "");

        Encounter encounterFromResponse = getEncounterFromResponse(response);
        Encounter encounterFromRepo = getObjectFromJpaRepo(createdEncounterId, encounterRepo);

        assertThat(encounterFromResponse.getDescription()).isEqualTo("");
        assertThat(encounterFromResponse).isEqualTo(encounterFromRepo).usingRecursiveComparison()
                .ignoringFields("description")
                .isEqualTo(createdEncounter);
    }

    @Test
    void userWithEncountersCanBeDeleted() {
        Integer userId = readObjectFromResponse(
                createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK), User.class).getId();
        enableUserAccount(userId);

        int createdEncounterId1 = createEncounter(userId, "test").getId();
        int createdEncounterId2 = createEncounter(userId, "test").getId();

        assertThat(deleteUser(userId)).isEqualTo(1);

        for (Integer encId : List.of(createdEncounterId1, createdEncounterId2)) {
            Optional<Encounter> encounter = encounterRepo.findById(encId);
            assertThat(encounter).isPresent();
            assertThat(encounter.get().getUserId()).isNull();
        }
    }


    private Encounter createEncounter(Integer userId, String description) {
        MockHttpServletResponse response = createEncounter(userId, description, HttpStatus.OK);
        return getEncounterFromResponse(response);
    }

    @SneakyThrows
    private MockHttpServletResponse createEncounter(Integer userId, String description, HttpStatus expectedStatus) {

        MockHttpServletResponse response = sendRequest(HttpMethod.POST, expectedStatus, TEST_USERNAME_ADMIN_1,
                "/encounter", ow.writeValueAsString(new EncounterDto(ENC_NAME, userId, description)));

        try {
            Encounter encounter = mapper.readValue(response.getContentAsString(), Encounter.class);
            if (encounter.getId() != null) {
                createdEncounterIds.add(encounter.getId());
            }
        } catch (Exception ignored) {
        }

        return response;
    }

    @SneakyThrows
    private MockHttpServletResponse getResponseForGetEncounterRequest(
            Integer encounterId, String username, HttpStatus expectedStatus) {

        return sendRequest(HttpMethod.GET, expectedStatus, username,
                "/encounter/" + encounterId, "");
    }

    @SneakyThrows
    private void publishUnpublishEncounter(int encounterId) {
        sendRequest(HttpMethod.PATCH, HttpStatus.OK, TEST_USERNAME_ADMIN_1,
                "/encounter/published/" + encounterId, "");
    }

    @SneakyThrows
    private Encounter getEncounterFromResponse(MockHttpServletResponse response) {
        return readObjectFromResponse(response, Encounter.class);
    }

}