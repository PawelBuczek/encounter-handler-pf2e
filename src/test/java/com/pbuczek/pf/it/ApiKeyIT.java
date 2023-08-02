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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
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
    }


    @Test
    @SneakyThrows
    void apiKeyIsCreatedCorrectly() {
        int userId = createUser(TEST_USERNAME_STANDARD_1, TEST_EMAIL_STANDARD_1);
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
