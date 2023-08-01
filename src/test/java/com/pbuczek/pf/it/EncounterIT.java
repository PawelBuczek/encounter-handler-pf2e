package com.pbuczek.pf.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pbuczek.pf.TestUserDetails;
import com.pbuczek.pf.encounter.Encounter;
import com.pbuczek.pf.encounter.EncounterDto;
import com.pbuczek.pf.encounter.EncounterRepository;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserDto;
import com.pbuczek.pf.user.UserRepository;
import com.pbuczek.pf.user.UserType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static com.pbuczek.pf.encounter.Encounter.MAX_DESCRIPTION_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EncounterIT implements TestUserDetails {

    private final List<Integer> createdUserIds = new ArrayList<>();
    private final List<Integer> createdEncounterIds = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static ObjectWriter ow;
    private static final String encName = "testEncounterName";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private EncounterRepository encounterRepo;

    @BeforeAll
    static void initialize() {
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.registerModule(new JavaTimeModule());
        ow = mapper.writer().withDefaultPrettyPrinter();
    }

    @BeforeEach
    void setUp() {
        Integer potentialId = userRepo.getIdByUsername(TEST_USERNAME_ADMIN_1);
        if (potentialId != null) {
            userRepo.deleteUser(potentialId);
        }
        User admin = new User(TEST_USERNAME_ADMIN_1, TEST_EMAIL_ADMIN_1, TEST_PASSWORD);
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
    void encounterIsCreatedCorrectly() throws Exception {
        int userId = createUserAndGetId(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
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
    void cannotCreateEncounterWithDescriptionTooLong() throws Exception {
        int userId = createUserAndGetId(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MockHttpServletResponse response = getResultActionsForCreatingEncounter(userId,
                RandomStringUtils.random(3001, true, true))
                .andExpect(status().isBadRequest()).andReturn().getResponse();

        assertThat(response).isNotNull();
        assertThat(response.getErrorMessage()).isEqualTo(
                String.format("description too long. Max '%d' signs allowed.", MAX_DESCRIPTION_LENGTH));
    }

    @Test
    void differentStandardUserCannotReadEncounterThatIsNotPublished() throws Exception {
        int userId = createUserAndGetId(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MvcResult postResult = getResultActionsForCreatingEncounter(userId, "test")
                .andExpect(status().isOk()).andReturn();

        Encounter createdEncounter = mapper.readValue(postResult.getResponse().getContentAsString(), Encounter.class);
        basicEncounterChecks(createdEncounter);
        Integer createdEncounterId = createdEncounter.getId();

        int secondUserId = createUserAndGetId(TEST_USERNAME_STANDARD_2, TEST_EMAIL_STANDARD_2);
        enableUserAccount(secondUserId);

        assertThat(this.mockMvc.perform(
                        get("/encounter/" + createdEncounterId)
                                .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_STANDARD_2)))
                .andExpect(status().isForbidden()).andReturn().getResponse().getErrorMessage())
                .isEqualTo("not authorized for this resource");
    }

    @Test
    void differentStandardUserCanReadEncounterThatIsPublished() throws Exception {
        int userId = createUserAndGetId(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        MvcResult postResult = getResultActionsForCreatingEncounter(userId, "test")
                .andExpect(status().isOk()).andReturn();

        Encounter createdEncounter = mapper.readValue(postResult.getResponse().getContentAsString(), Encounter.class);
        basicEncounterChecks(createdEncounter);
        Integer createdEncounterId = createdEncounter.getId();

        int secondUserId = createUserAndGetId(TEST_USERNAME_STANDARD_2, TEST_EMAIL_STANDARD_2);
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

    private int createUserAndGetId(String username, String email) throws Exception {
        MockHttpServletResponse response = this.mockMvc.perform(
                        post("/user")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ow.writeValueAsString(
                                        new UserDto(username, email, TestUserDetails.TEST_PASSWORD))))
                .andReturn().getResponse();
        User user = mapper.readValue(response.getContentAsString(), User.class);
        if (user.getId() != null) {
            createdUserIds.add(user.getId());
        } else {
            fail("User could not be created correctly");
        }
        return user.getId();
    }

    private ResultActions getResultActionsForCreatingEncounter(Integer userId, String description) throws Exception {
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

    private String getBasicAuthenticationHeader(String username) {
        String valueToEncode = username + ":" + TestUserDetails.TEST_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    private void enableUserAccount(int userId) throws Exception {
        this.mockMvc.perform(
                patch("/user/enable/" + userId)
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1)));
    }

    private void publishUnpublishEncounter(int encounterId) throws Exception {
        this.mockMvc.perform(
                patch("/encounter/published/" + encounterId)
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1)));
    }

}