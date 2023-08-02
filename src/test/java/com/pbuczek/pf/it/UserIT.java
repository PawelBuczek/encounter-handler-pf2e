package com.pbuczek.pf.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pbuczek.pf.TestUserDetails;
import com.pbuczek.pf.user.*;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserIT implements TestUserDetails {

    private final List<Integer> createdUserIds = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static ObjectWriter ow;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    UserRepository userRepo;

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
        createdUserIds.forEach(id -> userRepo.deleteUser(id));
    }

    @Test
    @SneakyThrows
    void userIsCreatedCorrectly() {
        MockHttpServletResponse response =
                createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK);

        User createdUser = getUserFromResponse(response);

        assertAll("Verify User properties",
                () -> assertThat(createdUser.getUsername()).isEqualTo(TEST_USERNAME_STANDARD_1),
                () -> assertThat(createdUser.getEmail()).isEqualTo(TEST_EMAIL_STANDARD_1),
                () -> assertThat(createdUser.getLocked()).isFalse(),
                () -> assertThat(createdUser.getEnabled()).isFalse(),
                () -> assertThat(createdUser.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now()),
                () -> assertThat(createdUser.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now()),
                () -> assertThat(createdUser.getPaymentPlan()).isEqualTo(PaymentPlan.FREE),
                () -> assertThat(createdUser.getType()).isEqualTo(UserType.STANDARD),
                () -> assertThat(createdUser.getPassword()).isEqualTo("[hidden for security reasons]"));

        Optional<User> optionalUser = userRepo.findById(createdUser.getId());
        assertThat(optionalUser).isPresent();
        assertThat(optionalUser.get()).usingRecursiveComparison()
                .ignoringFields("password").isEqualTo(createdUser);
    }

    @Test
    @SneakyThrows
    void userWithWrongUsernameWillNotBeCreated() {
        createUser(null, TEST_EMAIL_STANDARD_1, HttpStatus.UNPROCESSABLE_ENTITY,
                "provided username is empty.");
        createUser("", TEST_EMAIL_STANDARD_1, HttpStatus.UNPROCESSABLE_ENTITY,
                "provided username is empty.");

        String username;
        username = RandomStringUtils.random(User.MIN_USERNAME_LENGTH - 1, true, true);
        createUser(username, TEST_EMAIL_STANDARD_1, HttpStatus.UNPROCESSABLE_ENTITY,
                String.format("username needs to be between %d and %d characters.",
                        User.MIN_USERNAME_LENGTH, User.MAX_USERNAME_LENGTH));

        username = RandomStringUtils.random(User.MAX_USERNAME_LENGTH + 1, true, true);
        createUser(username, TEST_EMAIL_STANDARD_1, HttpStatus.UNPROCESSABLE_ENTITY,
                String.format("username needs to be between %d and %d characters.",
                        User.MIN_USERNAME_LENGTH, User.MAX_USERNAME_LENGTH));

        createUser(TEST_USERNAME_ADMIN_1, TEST_EMAIL_STANDARD_1, HttpStatus.CONFLICT,
                String.format("username '%s' is already being used by another user.", TEST_USERNAME_ADMIN_1));
    }

    @Test
    @SneakyThrows
    void userWithWrongEmailWillNotBeCreated() {
        createUser(TEST_USERNAME_STANDARD_1, null, HttpStatus.UNPROCESSABLE_ENTITY,
                "provided email is empty.");
        createUser(TEST_USERNAME_STANDARD_1, "", HttpStatus.UNPROCESSABLE_ENTITY,
                "provided email is empty.");
        createUser(TEST_USERNAME_STANDARD_1, "test", HttpStatus.UNPROCESSABLE_ENTITY,
                "provided user email 'test' is not valid.");
        createUser(TEST_USERNAME_STANDARD_1, "test@test.", HttpStatus.UNPROCESSABLE_ENTITY,
                "provided user email 'test@test.' is not valid.");

        createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_ADMIN_1, HttpStatus.CONFLICT,
                String.format("email '%s' is already being used by another user.", TEST_EMAIL_ADMIN_1));
    }

    @SneakyThrows
    private MockHttpServletResponse createUser(
            String username, String email, HttpStatus expectedStatus) {

        return this.mockMvc.perform(
                        post("/user")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ow.writeValueAsString(
                                        new UserDto(username, email, TestUserDetails.TEST_PASSWORD))))
                .andExpect(status().is(expectedStatus.value())).andReturn().getResponse();
    }

    @SneakyThrows
    private void createUser(
            String username, String email, HttpStatus expectedStatus, String expectedErrorMessage) {

        MockHttpServletResponse response = createUser(username, email, expectedStatus);
        assertThat(response.getErrorMessage()).isEqualTo(expectedErrorMessage);
    }

    @SneakyThrows
    private User getUserFromResponse(MockHttpServletResponse response) {
        User user = mapper.readValue(response.getContentAsString(), User.class);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotNull();
        createdUserIds.add(user.getId());

        return user;
    }
}