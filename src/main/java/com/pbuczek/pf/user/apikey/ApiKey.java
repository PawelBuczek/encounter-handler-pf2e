package com.pbuczek.pf.user.apikey;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Collectors;


@Data
@NoArgsConstructor
@Entity
@Table(name = "api_key")
public class ApiKey {

    public ApiKey(Integer userId) {
        this.userId = userId;
        this.validTillDate = LocalDate.now(ZoneOffset.UTC).plusYears(1);
        this.apiKeyValue = getTimeBasedPartOfApiKeyValue() + UUID.randomUUID();
        System.out.println(this.apiKeyValue);
    }

    private String getTimeBasedPartOfApiKeyValue() {
        return LocalDate.now(ZoneOffset.UTC).toString().chars()
                .mapToObj(ApiKey::intToRandomCapitalizationLetter)
                .collect(Collectors.joining());
    }

    private static String intToRandomCapitalizationLetter(int i) {
        int addition = Math.random() < 0.5 ? 17 : 49;
        return i >= 48 && i <= 57 ? String.valueOf((char) (i + addition)) : String.valueOf((char) i);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Nonnull
    private Integer userId;
    @Nonnull
    private LocalDate validTillDate;
    @Nonnull
    @Column(columnDefinition = "CHAR(66)")
    private String apiKeyValue;
}
