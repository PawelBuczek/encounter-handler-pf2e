package com.pbuczek.pf.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UnitTest")
class UserTest {

    @Test
    void userIsGeneratedCorrectly() {
        User user = new User("johndoe", "johndoe@example.com", "exPass@1");

        UserDto newUserDto = new UserDto();
        newUserDto.setUsername("johndoe");
        newUserDto.setEmail("johndoe@example.com");
        newUserDto.setPassword("exPass@1");
        User newUser = new User(newUserDto);

        assertThat(user.getId()).isEqualTo(newUser.getId()).isNull();
        assertThat(user.getUsername()).isEqualTo(newUser.getUsername()).isEqualTo("johndoe");
        assertThat(user.getEmail()).isEqualTo(newUser.getEmail()).isEqualTo("johndoe@example.com");
        assertThat(user.getLocked()).isEqualTo(newUser.getLocked()).isEqualTo(false);
        assertThat(user.getEnabled()).isEqualTo(newUser.getEnabled()).isEqualTo(false);
        assertThat(user.getTimeCreated())
                .isBeforeOrEqualTo(newUser.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(user.getPasswordLastUpdatedDate())
                .isBeforeOrEqualTo(newUser.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now());
        assertThat(user.getPaymentPlan()).isEqualTo(newUser.getPaymentPlan()).isEqualTo(PaymentPlan.FREE);
        assertThat(user.getType()).isEqualTo(newUser.getType()).isEqualTo(UserType.STANDARD);
        assertThat(user.getPassword()).hasSize(60)
                .doesNotContainAnyWhitespaces().hasLineCount(1);
        assertThat(newUser.getPassword()).hasSize(60)
                .doesNotContainAnyWhitespaces().hasLineCount(1);
    }

    @Test
    void refreshPasswordLastUpdatedDate() {
        User user = new User("johndoe", "johndoe@example.com", "exPass@1");

        user.setPasswordLastUpdatedDate(LocalDate.of(2020, 1, 1));
        user.refreshPasswordLastUpdatedDate();

        assertThat(user.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now());
    }
}