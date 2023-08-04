package com.pbuczek.pf.it;

import com.pbuczek.pf.user.PaymentPlan;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserType;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@Tag("IntegrationTest")
class UserIT extends _BaseIT {

    @AfterEach
    void tearDown() {
        createdUserIds.forEach(id -> userRepo.deleteUser(id));
    }


    @Test
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

        assertThat(getUserFromRepo(createdUser.getId())).usingRecursiveComparison()
                .ignoringFields("password").isEqualTo(createdUser);
    }

    @Test
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

    @Test
    void userCanBeDeleted() {
        Integer userId = getUserFromResponse(
                createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK)).getId();

        getUserFromRepo(userId);
        assertThat(deleteUser(userId)).isEqualTo(1);
        assertThat(deleteUser(userId)).isEqualTo(0);
        Optional<User> optionalUser = userRepo.findById(userId);
        assertThat(optionalUser).isEmpty();
    }

    @Test
    void userTypeIsInitiallyStandardAndCanBeUpdated() {
        Integer userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        User initialUser = getUserFromRepo(userId);

        assertThat(initialUser.getType()).isEqualTo(UserType.STANDARD);
        assertThat(updateUserType(userId, UserType.ADMIN).getType()).isEqualTo(UserType.ADMIN);
        assertThat(updateUserType(userId, UserType.STANDARD).getType()).isEqualTo(UserType.STANDARD);

        assertThat(initialUser).isEqualTo(getUserFromRepo(userId));
    }

    @Test
    void userPasswordLastUpdatedDateCanBeRefreshed() {
        Integer userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        User initialUser = getUserFromRepo(userId);
        assertThat(initialUser.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now());
        initialUser.setPasswordLastUpdatedDate(LocalDate.now().minusDays(1));
        userRepo.save(initialUser);

        assertThat(refreshPasswordLastUpdatedDate(userId).getPasswordLastUpdatedDate())
                .isEqualTo(getUserFromRepo(userId).getPasswordLastUpdatedDate())
                .isAfter(initialUser.getPasswordLastUpdatedDate());

        assertThat(initialUser).usingRecursiveComparison()
                .ignoringFields("passwordLastUpdatedDate").isEqualTo(getUserFromRepo(userId));
    }

    @Test
    void userIsInitiallyUnlockedAndCanBeLockedAndUnlocked() {
        Integer userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        User initialUser = getUserFromRepo(userId);
        assertThat(initialUser.getLocked()).isFalse();

        assertThat(lockUnlock(userId).getLocked()).isEqualTo(getUserFromRepo(userId).getLocked()).isTrue();
        assertThat(lockUnlock(userId).getLocked()).isEqualTo(getUserFromRepo(userId).getLocked()).isFalse();

        assertThat(initialUser).isEqualTo(getUserFromRepo(userId));
    }

    @Test
    void userPaymentPlanIsInitiallyFreeAndCanBeUpdated() {
        Integer userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        User initialUser = getUserFromRepo(userId);
        assertThat(initialUser.getPaymentPlan()).isEqualTo(PaymentPlan.FREE);

        assertThat(updatePaymentPlan(userId, PaymentPlan.HERO).getPaymentPlan())
                .isEqualTo(getUserFromRepo(userId).getPaymentPlan())
                .isEqualTo(PaymentPlan.HERO);
        assertThat(updatePaymentPlan(userId, PaymentPlan.FREE).getPaymentPlan())
                .isEqualTo(getUserFromRepo(userId).getPaymentPlan())
                .isEqualTo(PaymentPlan.FREE);

        assertThat(initialUser).isEqualTo(getUserFromRepo(userId));
    }

    @Test
    void userEmailCanBeUpdated() {
        Integer userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        User initialUser = getUserFromRepo(userId);
        assertThat(initialUser.getEmail()).isEqualTo(TEST_EMAIL_STANDARD_1);

        assertThat(updateEmail(userId, TEST_EMAIL_STANDARD_2).getEmail())
                .isEqualTo(getUserFromRepo(userId).getEmail()).isEqualTo(TEST_EMAIL_STANDARD_2);
        assertThat(updateEmail(userId, TEST_EMAIL_STANDARD_1)
                .getEmail()).isEqualTo(getUserFromRepo(userId).getEmail()).isEqualTo(TEST_EMAIL_STANDARD_1);

        assertThat(initialUser).isEqualTo(getUserFromRepo(userId));
    }

    private User updatePaymentPlan(Integer userId, PaymentPlan paymentPlan) {
        return sendAdminPatchRequest("/user/paymentplan/" + userId + "/" + paymentPlan);
    }

    private User lockUnlock(Integer userId) {
        return sendAdminPatchRequest("/user/lock-unlock/" + userId);
    }

    private User refreshPasswordLastUpdatedDate(Integer userId) {
        return sendAdminPatchRequest("/user/refresh-password-last-updated-date/" + userId);
    }

    private User updateUserType(Integer userId, UserType userType) {
        return sendAdminPatchRequest("/user/usertype/" + userId + "/" + userType);
    }

    @SneakyThrows
    private User sendAdminRequestUser(HttpMethod requestMethod, String url, String content) {
        return getUserFromResponse(
                this.mockMvc.perform(MockMvcRequestBuilders.request(requestMethod, url)
                                .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
                        .andReturn().getResponse());
    }

    private User sendAdminPatchRequest(String url, String content) {
        return sendAdminRequestUser(HttpMethod.PATCH, url, content);
    }

    private User sendAdminPatchRequest(String url) {
        return sendAdminRequestUser(HttpMethod.PATCH, url, "");
    }

    @SneakyThrows
    private User updateEmail(Integer userId, String email) {
        return sendAdminPatchRequest("/user/email/" + userId, email);
    }

    @SneakyThrows
    private int deleteUser(Integer userId) {
        return Integer.parseInt(this.mockMvc.perform(delete("/user/" + userId)
                .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1))).andReturn().getResponse().getContentAsString());
    }

    private User getUserFromRepo(Integer userId) {
        Optional<User> optionalUser = userRepo.findById(userId);
        assertThat(optionalUser).isPresent();
        return optionalUser.get();
    }

    private User getUserFromResponse(MockHttpServletResponse response) {
        return getObjectFromResponse(response, User.class, createdUserIds);
    }
}