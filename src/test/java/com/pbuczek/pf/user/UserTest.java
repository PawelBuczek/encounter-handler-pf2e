package com.pbuczek.pf.user;

import com.pbuczek.pf.TestUserDetails;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag("UnitTest")
class UserTest implements TestUserDetails {

    private final String TEST_USERNAME_ADMIN_1 = "_Test_" + LocalDate.now() + RandomStringUtils.random(24, true, true);
    private final String TEST_EMAIL_ADMIN_1 = TEST_USERNAME_ADMIN_1 + "@test.com";
    private final String TEST_PASSWORD = "aB@1" + RandomStringUtils.random(50);

    @Test
    void userIsGeneratedCorrectly() {
        User user = new User(TEST_USERNAME_ADMIN_1, TEST_EMAIL_ADMIN_1, TEST_PASSWORD);
        User newUser = new User(new UserDto(TEST_USERNAME_ADMIN_1, TEST_EMAIL_ADMIN_1, TEST_PASSWORD));

        assertAll("Verify users properties",
                () -> assertThat(user.getId()).isEqualTo(newUser.getId()).isNull(),
                () -> assertThat(user.getUsername()).isEqualTo(newUser.getUsername()).isEqualTo(TEST_USERNAME_ADMIN_1),
                () -> assertThat(user.getEmail()).isEqualTo(newUser.getEmail()).isEqualTo(TEST_EMAIL_ADMIN_1),
                () -> assertThat(user.getLocked()).isEqualTo(newUser.getLocked()).isFalse(),
                () -> assertThat(user.getEnabled()).isEqualTo(newUser.getEnabled()).isFalse(),
                () -> assertThat(user.getPaymentPlan()).isEqualTo(newUser.getPaymentPlan()).isEqualTo(PaymentPlan.FREE),
                () -> assertThat(user.getType()).isEqualTo(newUser.getType()).isEqualTo(UserType.STANDARD),
                () -> assertThat(user.getTimeCreated())
                        .isBeforeOrEqualTo(newUser.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now()),
                () -> assertThat(user.getPasswordLastUpdatedDate())
                        .isBeforeOrEqualTo(newUser.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now()),
                () -> assertThat(user.getPassword()).hasSize(60)
                        .doesNotContainAnyWhitespaces().hasLineCount(1),
                () -> assertThat(newUser.getPassword()).hasSize(60)
                        .doesNotContainAnyWhitespaces().hasLineCount(1));
    }

    @Test
    void refreshPasswordLastUpdatedDate() {
        User user = new User(TEST_USERNAME_ADMIN_1, TEST_EMAIL_ADMIN_1, TEST_PASSWORD);

        user.setPasswordLastUpdatedDate(LocalDate.of(2020, 1, 1));
        user.refreshPasswordLastUpdatedDate();

        assertThat(user.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now());
    }
}