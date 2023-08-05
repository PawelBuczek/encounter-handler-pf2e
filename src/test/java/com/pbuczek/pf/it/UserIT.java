package com.pbuczek.pf.it;

import com.fasterxml.jackson.core.JsonParser;
import com.pbuczek.pf.user.PaymentPlan;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserType;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        initialUser.setPasswordLastUpdatedDate(LocalDate.now().minusDays(2));
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

    @Test
    void usernameCanBeUpdated() {
        Integer userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        User initialUser = getUserFromRepo(userId);
        assertThat(initialUser.getUsername()).isEqualTo(TEST_USERNAME_STANDARD_1);

        assertThat(updateUsername(userId, TEST_USERNAME_STANDARD_2 + " ").getUsername())
                .isEqualTo(getUserFromRepo(userId).getUsername())
                .isEqualTo(TEST_USERNAME_STANDARD_2);
        assertThat(updateUsername(userId, TEST_USERNAME_STANDARD_1).getUsername())
                .isEqualTo(getUserFromRepo(userId).getUsername())
                .isEqualTo(TEST_USERNAME_STANDARD_1);

        assertThat(initialUser).isEqualTo(getUserFromRepo(userId));
    }

    @Test
    void userCanBeFound() {
        User initialUser = getUserFromRepo(createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1));

        assertThat(initialUser).usingRecursiveComparison()
                .ignoringFields("password")
                .isEqualTo(getUserFromResponse(sendAdminGetRequest(
                        HttpStatus.OK, "/user/by-userid/" + initialUser.getId(), "")));

        assertThat(initialUser).usingRecursiveComparison()
                .ignoringFields("password")
                .isEqualTo(getUserFromResponse(sendAdminGetRequest(
                        HttpStatus.OK, "/user/by-username/" + initialUser.getUsername(), "")));
    }

    @SneakyThrows
    @Test
    //CORRECT THIS
    void allUsersCanBeFoundByAdminOnly() {
        enableUserAccount(createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1));

        @SuppressWarnings("unchecked")
        List<User> list = getObjectFromResponse(sendAdminGetRequest(HttpStatus.OK, "/user", ""), List.class);

        User admin = getUserFromRepo(userRepo.getIdByUsername(TEST_USERNAME_ADMIN_1));
        User standard = getUserFromRepo(userRepo.getIdByUsername(TEST_USERNAME_STANDARD_1));
        admin.setPassword("[hidden for security reasons]");
        standard.setPassword("[hidden for security reasons]");


        assertThat(list).containsAll(List.of(admin, standard));

        sendRequest(HttpMethod.GET, HttpStatus.FORBIDDEN, TEST_USERNAME_STANDARD_1, "/user", "");
    }

    @Test
    void userPasswordIsNotReturned() {
        User user = getUserFromResponse(createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK));

        assertThat(user.getPassword()).isEqualTo("[hidden for security reasons]");
    }

    @SneakyThrows
    @Test
    void userCanUpdateHisOwnPassword() {
        User initialUser =
                getUserFromResponse(createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1, HttpStatus.OK));
        enableUserAccount(initialUser.getId());
        initialUser.setEnabled(true);

        String newPassword = "aB@1" + RandomStringUtils.random(49);
        JSONObject passwordDto = new JSONObject();
        passwordDto.put("currentPassword", TEST_PASSWORD);
        passwordDto.put("newPassword", newPassword);

        sendRequest(HttpMethod.PATCH, HttpStatus.OK, TEST_USERNAME_STANDARD_1, "/user/password", passwordDto.toString());

        String valueToEncode = TEST_USERNAME_STANDARD_1 + ":" + newPassword;
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());

        MockHttpServletResponse response = this.mockMvc.perform(MockMvcRequestBuilders.request(
                                HttpMethod.GET, "/user/by-userid/" + initialUser.getId())
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(getUserFromResponse(response)).isEqualTo(initialUser).usingRecursiveComparison()
                .ignoringFields("password").isEqualTo(getUserFromRepo(initialUser.getId()));
    }


    private User updatePaymentPlan(Integer userId, PaymentPlan paymentPlan) {
        return getUserFromResponse(sendAdminPatchRequest(
                HttpStatus.OK, "/user/paymentplan/" + userId + "/" + paymentPlan, ""));
    }

    private User refreshPasswordLastUpdatedDate(Integer userId) {
        return getUserFromResponse(sendAdminPatchRequest(
                HttpStatus.OK, "/user/refresh-password-last-updated-date/" + userId, ""));
    }

    private User lockUnlock(Integer userId) {
        return getUserFromResponse(sendAdminPatchRequest(
                HttpStatus.OK, "/user/lock-unlock/" + userId, ""));
    }

    private User updateUserType(Integer userId, UserType userType) {
        return getUserFromResponse(sendAdminPatchRequest(
                HttpStatus.OK, "/user/usertype/" + userId + "/" + userType, ""));
    }

    private User updateEmail(Integer userId, String email) {
        return getUserFromResponse(sendAdminPatchRequest(
                HttpStatus.OK, "/user/email/" + userId, email));
    }

    private User updateUsername(Integer userId, String username) {
        return getUserFromResponse(sendAdminPatchRequest(
                HttpStatus.OK, "/user/username/" + userId + "/" + username, ""));
    }

    private User getUserFromResponse(MockHttpServletResponse response) {
        return getObjectFromResponse(response, User.class, createdUserIds);
    }

    private User getUserFromRepo(Integer userId) {
        return getObjectFromJpaRepo(userId, userRepo);
    }

    @SneakyThrows
    private int deleteUser(Integer userId) {
        return Integer.parseInt(
                sendAdminDeleteRequest(HttpStatus.OK, "/user/" + userId, "").getContentAsString());
    }
}