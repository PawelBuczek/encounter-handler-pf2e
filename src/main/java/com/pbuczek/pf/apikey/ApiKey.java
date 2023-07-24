package com.pbuczek.pf.apikey;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.stream.Collectors;

import static com.pbuczek.pf.security.SecurityHelper.passwordEncoder;


@Data
@NoArgsConstructor
@Entity
@Table(name = "api_key")
public class ApiKey {

    private static final int IDENTIFIER_LENGTH = 35;

    public ApiKey(String apiKeyValue, Integer userId) {
        this.userId = userId;
        this.identifier = createUniqueIdentifier();
        System.out.println("identifier=" + identifier);
        System.out.println("apiKeyValue=" + apiKeyValue);
        this.apiKeyValue = passwordEncoder.encode(apiKeyValue);
        this.validTillDate = LocalDate.now(ZoneOffset.UTC).plusYears(1);
    }

    private String createUniqueIdentifier() {
        String currentTimeUTC = LocalDateTime.now(ZoneOffset.UTC).toString();
        String userIdLast6Digits = this.userId.toString().substring(
                Math.max(0, this.userId.toString().length() - 6));

        String timePlusUserId = currentTimeUTC + String.format("%06d", Integer.valueOf(userIdLast6Digits));

        StringBuilder uniqueIdWithRandomCapitalization = new StringBuilder(
                timePlusUserId.chars()
                        .mapToObj(ApiKey::intToRandomCapitalizationLetter)
                        .collect(Collectors.joining())
        );

        Random random = new Random();
        while (uniqueIdWithRandomCapitalization.length() < IDENTIFIER_LENGTH) {
            char randomChar = (char) (random.nextInt(60) + 'A');
            uniqueIdWithRandomCapitalization.append(randomChar);
        }

        return uniqueIdWithRandomCapitalization.toString();
    }


    private static String intToRandomCapitalizationLetter(int i) {
        int addition = Math.random() < 0.5 ? 17 : 49;
        return i >= 48 && i <= 57 ? String.valueOf((char) (i + addition)) : String.valueOf((char) i);
    }

    @Id
    @Column(columnDefinition = "CHAR(35)")
    private String identifier;
    @Nonnull
//    @Column(columnDefinition = "CHAR(36)") //this would be before encryption
    @Column(columnDefinition = "CHAR(60)")
    private String apiKeyValue;
    @Nonnull
    private Integer userId;
    @Nonnull
    private LocalDate validTillDate;
}
