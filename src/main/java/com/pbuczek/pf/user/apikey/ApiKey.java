package com.pbuczek.pf.user.apikey;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;


@Data
@NoArgsConstructor
@Entity
@Table(name = "api_key")
public class ApiKey {

    public ApiKey(Integer userId) {
        this.userId = userId;
        this.timeCreated = LocalDateTime.now(ZoneOffset.UTC);
        this.apiKeyValue = this.timeCreated + "%" + UUID.randomUUID();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Nonnull
    private Integer userId;
    @Nonnull
    private LocalDateTime timeCreated;
    @Nonnull
    private String apiKeyValue;
}
