package com.pbuczek.pf.it;

import com.pbuczek.pf.encounter.Encounter;
import com.pbuczek.pf.user.PaymentPlan;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserType;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag("IntegrationTest")
class UserIT extends _BaseIT {

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
    private User getUserFromResponse(MockHttpServletResponse response) {
        return getObjectFromResponse(response, User.class, createdUserIds);
    }
}