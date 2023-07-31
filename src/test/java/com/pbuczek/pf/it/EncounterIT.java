package com.pbuczek.pf.it;

import com.pbuczek.pf.TestUserDetails;
import com.pbuczek.pf.encounter.Encounter;
import com.pbuczek.pf.encounter.EncounterDto;
import com.pbuczek.pf.encounter.EncounterRepository;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserDto;
import com.pbuczek.pf.user.UserRepository;
import com.pbuczek.pf.user.UserType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("IntegrationTest")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EncounterIT implements TestUserDetails {

    private final List<Integer> createdUserIds = new ArrayList<>();
    private final List<Integer> createdEncounterIds = new ArrayList<>();

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepo;
    @Autowired
    EncounterRepository encounterRepo;

    @BeforeEach
    void setUp() {
        Integer potentialId = userRepo.getIdByUsername(TEST_USERNAME_1);
        if (potentialId != null) {
            userRepo.deleteUser(potentialId);
        }
        User admin = new User(TEST_USERNAME_1, TEST_EMAIL_1, TEST_PASSWORD);
        admin.setType(UserType.ADMIN);
        admin.setEnabled(true);
        userRepo.save(admin);
        createdUserIds.add(admin.getId());
    }

    @AfterEach
    void tearDown() {
        createdEncounterIds.forEach(id -> encounterRepo.deleteEncounter(id));
        createdUserIds.forEach(id -> userRepo.deleteUser(id));
    }

    @Test
    void encounterIsCreatedCorrectly() {
        int userId = createUserAndGetId(TEST_USERNAME_2, TEST_EMAIL_2);
        ResponseEntity<Encounter> response = getResponseForCreatingEncounter("test", userId, "test");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().isInline()).isFalse();

        Encounter createdEncounter = response.getBody();
        assert createdEncounter != null;
        assertThat(createdEncounter.getId()).isNotNull();
        createdEncounterIds.add(createdEncounter.getId());

        assertAll("Verify createdEncounter properties",
                () -> assertThat(createdEncounter.getName()).isEqualTo("test"),
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
        int userId = createUserAndGetId(TEST_USERNAME_2, TEST_EMAIL_2);
        ResponseEntity<Encounter> response = getResponseForCreatingEncounter("test", userId,
                RandomStringUtils.random(3001, true, true));
        System.out.println(response);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void differentUserCannotReadEncounterThatIsNotPublished() {

    }

    @Test
    void differentUserCanReadEncounterThatIsPublished() {

    }

    private int createUserAndGetId(String username, String email) {
        RequestEntity<UserDto> request = RequestEntity
                .post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserDto(username, email, TestUserDetails.TEST_PASSWORD));
        ResponseEntity<User> response = restTemplate.exchange(request, User.class);
        if (response != null && response.getBody() != null && response.getBody().getId() != null) {
            createdUserIds.add(response.getBody().getId());
        } else {
            fail("User could not be created correctly");
        }
        return response.getBody().getId();
    }

    private ResponseEntity<Encounter> getResponseForCreatingEncounter(String name, Integer userId, String description) {
        RequestEntity<EncounterDto> request = RequestEntity
                .post("/encounter")
                .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_1, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EncounterDto(name, userId, description));
        return restTemplate.exchange(request, Encounter.class);
    }

    private String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}