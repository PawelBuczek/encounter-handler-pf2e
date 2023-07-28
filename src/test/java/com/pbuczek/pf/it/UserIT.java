package com.pbuczek.pf.it;

import com.pbuczek.pf.user.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
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

@Tag("IntegrationTest")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserIT {

    private final List<Integer> createdUserIds = new ArrayList<>();
    private static final String ADMIN_PASSWORD = "exPass@1" + RandomStringUtils.random(40);
    private static final String ADMIN_USERNAME = "integrationTestAdmin";
    private static final String ADMIN_EMAIL = "integrationTestAdmin@test.com";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepo;

    @BeforeEach
    void setUp() {
        Integer potentialId = userRepo.getIdByUsername(ADMIN_USERNAME);
        if (potentialId != null) {
            userRepo.deleteUserByUserId(potentialId);
        }
        User user = new User(ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD);
        userRepo.save(user);
        createdUserIds.add(user.getId());
    }

    @AfterEach
    void tearDown() {
        createdUserIds.forEach(id -> userRepo.deleteUserByUserId(id));
    }

    @Test
    void userIsCreatedCorrectly() {
        UserDto userDto = new UserDto();
        userDto.setUsername("userIsCreatedCorrectly");
        userDto.setEmail("userIsCreatedCorrectly@test.com");
        userDto.setPassword("exPass@1" + RandomStringUtils.random(40));

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

        assertThat(createdUser.getUsername()).isEqualTo(userDto.getUsername());
        assertThat(createdUser.getEmail()).isEqualTo(userDto.getEmail());
        assertThat(createdUser.getLocked()).isEqualTo(false);
        assertThat(createdUser.getEnabled()).isEqualTo(false);
        assertThat(createdUser.getTimeCreated()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(createdUser.getPasswordLastUpdatedDate()).isBeforeOrEqualTo(LocalDate.now());
        assertThat(createdUser.getPaymentPlan()).isEqualTo(PaymentPlan.FREE);
        assertThat(createdUser.getType()).isEqualTo(UserType.STANDARD);
        assertThat(createdUser.getPassword()).isEqualTo("[hidden for security reasons]");

        Optional<User> optionalUser = userRepo.findById(createdUser.getId());
        assertThat(optionalUser).isPresent();
        User retrievedUser = optionalUser.get();
        //for some reason nanoseconds get cut off when retrieving from database.
        assertThat(retrievedUser.getTimeCreated()).isEqualToIgnoringNanos(createdUser.getTimeCreated());
        retrievedUser.setTimeCreated(createdUser.getTimeCreated());
        retrievedUser.setPassword("[hidden for security reasons]");
        assertThat(createdUser).isEqualTo(retrievedUser);
    }
}