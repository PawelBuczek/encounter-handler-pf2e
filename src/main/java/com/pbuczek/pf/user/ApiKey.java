package com.pbuczek.pf.user;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Nonnull
    private Integer userId;
    @Nonnull
    @Column(columnDefinition = "CHAR(32)")
    private String apiKeyValue;
}
