package com.pbuczek.pf.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pbuczek.pf.TestUserDetails;
import com.pbuczek.pf.apikey.ApiKey;
import com.pbuczek.pf.apikey.ApiKeyRepository;
import com.pbuczek.pf.user.*;
import lombok.SneakyThrows;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ApiKeyIT implements TestUserDetails {

    private final List<Integer> createdUserIds = new ArrayList<>();
    private final List<String> createdApiKeyIdentifiers = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static ObjectWriter ow;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    UserRepository userRepo;
    @Autowired
    ApiKeyRepository apiRepo;

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
        createdApiKeyIdentifiers.forEach(identifier -> apiRepo.deleteApiKeyByIdentifier(identifier));
        createdUserIds.forEach(id -> userRepo.deleteUser(id));
    }


    @Test
    @SneakyThrows
    void apiKeyIsCreatedCorrectly() {
        int userId = createUserAndGetId(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        enableUserAccount(userId);
        changeUserPaymentPlan(userId, PaymentPlan.ADVENTURER);

        String apiKeyReceivedPass = createApiKey(HttpStatus.OK, TEST_USERNAME_STANDARD_1);
        assertThat(apiKeyReceivedPass).hasSize(71);

        ApiKey apiKey = getApiKeyFromRepo(apiKeyReceivedPass);

        assertAll("Verify ApiKey properties",
                () -> assertThat(apiKey.getUserId()).isEqualTo(userId),
                () -> assertThat(apiKey.getValidTillDate()).isBeforeOrEqualTo(LocalDate.now().plusYears(1)),
                () -> assertThat(apiKey.getIdentifier()).isNotEmpty().isAlphanumeric().hasSize(ApiKey.IDENTIFIER_LENGTH)
                        .doesNotContainAnyWhitespaces().hasLineCount(1),
                () -> assertThat(apiKey.getApiKeyValue()).hasSize(60)
                        .doesNotContainAnyWhitespaces().hasLineCount(1));
    }


    private int createUserAndGetId(String username, String email) throws Exception {
        MockHttpServletResponse response = this.mockMvc.perform(
                        post("/user")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ow.writeValueAsString(
                                        new UserDto(username, email, TestUserDetails.TEST_PASSWORD))))
                .andReturn().getResponse();
        User user = mapper.readValue(response.getContentAsString(), User.class);
        if (user.getId() != null) {
            createdUserIds.add(user.getId());
        } else {
            fail("User could not be created correctly");
        }
        return user.getId();
    }

    @SneakyThrows
    private void enableUserAccount(int userId) {
        this.mockMvc.perform(
                patch("/user/enable/" + userId)
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1)));
    }

    @SneakyThrows
    private void changeUserPaymentPlan(Integer userid, PaymentPlan plan) {
        this.mockMvc.perform(
                patch("/user/paymentplan/" + userid + "/" + plan)
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1)));
    }

    private String getBasicAuthenticationHeader(String username) {
        String valueToEncode = username + ":" + TestUserDetails.TEST_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    @SneakyThrows
    private String createApiKey(HttpStatus expectedStatus, String username) {
        return this.mockMvc.perform(post("/apikey")
                        .header("Authorization", getBasicAuthenticationHeader(username)))
                .andExpect(status().is(expectedStatus.value())).andReturn().getResponse().getContentAsString();
    }


    private ApiKey getApiKeyFromRepo(String apiKeyReceivedPass) {
        Optional<ApiKey> optionalApiKey =
                apiRepo.findByIdentifier(apiKeyReceivedPass.substring(0, ApiKey.IDENTIFIER_LENGTH));
        assertThat(optionalApiKey).isPresent();
        ApiKey apiKey = optionalApiKey.get();
        basicApiKeyChecks(apiKey);
        return apiKey;
    }

    private void basicApiKeyChecks(ApiKey apiKey) {
        assert apiKey != null;
        assertThat(apiKey.getIdentifier()).isNotNull();
        createdApiKeyIdentifiers.add(apiKey.getIdentifier());
    }

}
