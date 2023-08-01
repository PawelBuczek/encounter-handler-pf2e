package com.pbuczek.pf.it;

import com.pbuczek.pf.TestUserDetails;
import com.pbuczek.pf.user.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag("IntegrationTest")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserIT implements TestUserDetails {

    private final List<Integer> createdUserIds = new ArrayList<>();

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepo;

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
    void userIsCreatedCorrectly() {
        ResponseEntity<User> response = getResponseForCreatingUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);

        assertThat(response).isNotNull();
        User createdUser = response.getBody();
        assert createdUser != null;
        assertThat(createdUser.getId()).isNotNull();
        createdUserIds.add(createdUser.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().isInline()).isFalse();

        assertAll("Verify createdUser properties",
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
    void duplicateUserWillNotBeCreated() {
        ResponseEntity<User> response;

        response = getResponseForCreatingUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_ADMIN_1);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        response = getResponseForCreatingUser(TEST_USERNAME_ADMIN_1, TEST_EMAIL_STANDARD_1);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        response = getResponseForCreatingUser(TEST_USERNAME_ADMIN_1, TEST_EMAIL_ADMIN_1);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void userWithWrongUsernameWillNotBeCreated() {
        ResponseEntity<User> response;

        response = getResponseForCreatingUser(null, TEST_EMAIL_STANDARD_1);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        response = getResponseForCreatingUser("", TEST_EMAIL_STANDARD_1);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        response = getResponseForCreatingUser("ab", TEST_EMAIL_STANDARD_1);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        response = getResponseForCreatingUser(
                TEST_USERNAME_STANDARD_1 + RandomStringUtils.random(40, true, true), TEST_EMAIL_STANDARD_1);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void userWithWrongEmailWillNotBeCreated() {
        ResponseEntity<User> response;

        response = getResponseForCreatingUser(TEST_USERNAME_STANDARD_1, null);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        response = getResponseForCreatingUser(TEST_USERNAME_STANDARD_1, "");
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        response = getResponseForCreatingUser(TEST_USERNAME_STANDARD_1, "test");
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        response = getResponseForCreatingUser(TEST_USERNAME_STANDARD_1, "test@test.");
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void canAuthenticateWithApiKey() {

    }


    private ResponseEntity<User> getResponseForCreatingUser(String username, String email) {
        RequestEntity<UserDto> request = RequestEntity
                .post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserDto(username, email, TestUserDetails.TEST_PASSWORD));
        return restTemplate.exchange(request, User.class);
    }
}