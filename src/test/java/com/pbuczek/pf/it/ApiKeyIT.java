package com.pbuczek.pf.it;

import com.pbuczek.pf.apikey.ApiKey;
import com.pbuczek.pf.apikey.ApiKeyRepository;
import com.pbuczek.pf.user.PaymentPlan;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.pbuczek.pf.apikey.ApiKeyController.API_KEY_LIMITS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
public class ApiKeyIT extends _BaseIT {

    private final List<String> createdApiKeyIdentifiers = new ArrayList<>();

    @Autowired
    private ApiKeyRepository apiRepo;

    @AfterEach
    void tearDown() {
        createdApiKeyIdentifiers.forEach(identifier -> apiRepo.deleteApiKeyByIdentifier(identifier));
        createdUserIds.forEach(id -> userRepo.deleteUser(id));
    }


    @Test
    void apiKeyIsCreatedCorrectly() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        enableUserAccount(userId);
        changeUserPaymentPlan(userId, PaymentPlan.ADVENTURER);

        String apiKeyReceivedPass = createApiKey(HttpStatus.OK);
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

    @Test
    void apiKeyCanBeDeleted() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        enableUserAccount(userId);
        changeUserPaymentPlan(userId, PaymentPlan.ADVENTURER);

        String apiKeyReceivedPass = createApiKey(HttpStatus.OK);

        deleteApiKey(userId, apiKeyReceivedPass.substring(0, ApiKey.IDENTIFIER_LENGTH));

        Optional<ApiKey> optionalApiKey =
                apiRepo.findByIdentifier(apiKeyReceivedPass.substring(0, ApiKey.IDENTIFIER_LENGTH));
        assertThat(optionalApiKey).isEmpty();
    }


    @Test
    void apiKeysCannotBeCreatedOverLimit() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
        enableUserAccount(userId);

        API_KEY_LIMITS.forEach((paymentPlan, limit) -> {
            changeUserPaymentPlan(userId, paymentPlan);
            for (int i = 0; i < limit; i++) {
                apiRepo.save(new ApiKey(UUID.randomUUID().toString(), userId));
            }
            createApiKey(HttpStatus.BAD_REQUEST);

            List<ApiKey> apiKeysList = apiRepo.findByUserId(userId);

            apiKeysList.forEach(apiKey -> apiRepo.deleteApiKeyByIdentifier(apiKey.getIdentifier()));
        });
    }


    @SneakyThrows
    private String createApiKey(HttpStatus expectedStatus) {
        return this.mockMvc.perform(post("/apikey")
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_STANDARD_1)))
                .andExpect(status().is(expectedStatus.value())).andReturn().getResponse().getContentAsString();
    }

    @SneakyThrows
    private void deleteApiKey(Integer userId, String identifier) {
        this.mockMvc.perform(delete("/apikey/" + userId + "/" + identifier)
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_STANDARD_1)))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse();
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
