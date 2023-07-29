package com.pbuczek.pf.it;

import com.pbuczek.pf.TestUserDetails;
import com.pbuczek.pf.user.*;
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
        Integer potentialId = userRepo.getIdByUsername(TEST_USERNAME_1);
        if (potentialId != null) {
            userRepo.deleteUserByUserId(potentialId);
        }
        User admin = new User(TEST_USERNAME_1, TEST_EMAIL_1, TEST_PASSWORD);
        admin.setType(UserType.ADMIN);
        admin.setEnabled(true);
        userRepo.save(admin);
        createdUserIds.add(admin.getId());
    }

    @AfterEach
    void tearDown() {
        createdUserIds.forEach(id -> userRepo.deleteUserByUserId(id));
    }

    @Test
    void userIsCreatedCorrectly() {
        UserDto userDto = new UserDto(TEST_USERNAME_2, TEST_EMAIL_2, TEST_PASSWORD);

        RequestEntity<UserDto> request = RequestEntity
                .post("/user")
//                .header("Authorization", "...")  // Not required for this endpoint
                .contentType(MediaType.APPLICATION_JSON)
                .body(userDto);

        ResponseEntity<User> response = restTemplate.exchange(request, User.class);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().isInline()).isFalse();

        User createdUser = response.getBody();
        assert createdUser != null;
        assertThat(createdUser.getId()).isNotNull();
        createdUserIds.add(createdUser.getId());

        assertAll("Verify createdUser properties",
                () -> assertThat(createdUser.getUsername()).isEqualTo(TEST_USERNAME_2),
                () -> assertThat(createdUser.getEmail()).isEqualTo(TEST_EMAIL_2),
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
}