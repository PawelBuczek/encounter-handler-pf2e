package com.pbuczek.pf.apikey;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UnitTest")
class ApiKeyTest {

    @Test
    void apiKeyIsGeneratedCorrectly() {
        ApiKey apiKey = new ApiKey(UUID.randomUUID().toString(), 1);

        assertThat(apiKey.getUserId()).isEqualTo(1);
        assertThat(apiKey.getIdentifier()).isNotEmpty().isAlphanumeric().hasSize(35)
                .doesNotContainAnyWhitespaces().hasLineCount(1);
        assertThat(apiKey.getValidTillDate()).isBeforeOrEqualTo(LocalDate.now().plusYears(1));
        assertThat(apiKey.getApiKeyValue()).hasSize(60)
                .doesNotContainAnyWhitespaces().hasLineCount(1);
    }
}