package com.pbuczek.pf.user;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void userIsGeneratedCorrectly() {
        User user = new User("johndoe", "johndoe@example.com", "exPass@1");

        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isEqualTo("johndoe");
        assertThat(user.getEmail()).isEqualTo("johndoe@example.com");
        assertThat(user.getLocked()).isEqualTo(false);
        assertThat(user.getEnabled()).isEqualTo(false);
        assertThat(user.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(user.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now());
        assertThat(user.getPaymentPlan()).isEqualTo(PaymentPlan.FREE);
        assertThat(user.getType()).isEqualTo(UserType.STANDARD);
        assertThat(user.getPassword()).hasSize(60)
                .doesNotContainAnyWhitespaces().hasLineCount(1);
    }

    @Test
    void refreshPasswordLastUpdatedDate() {
        User user = new User("johndoe", "johndoe@example.com", "exPass@1");

        user.setPasswordLastUpdatedDate(LocalDate.of(2020,1,1));
        user.refreshPasswordLastUpdatedDate();

        assertThat(user.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now());
    }
}