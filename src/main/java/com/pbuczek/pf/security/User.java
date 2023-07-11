package com.pbuczek.pf.security;

import com.pbuczek.pf.security.dto.UserDto;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {

    public User(UserType type, String username, String email, LocalDateTime timeCreated) {
        this.type = type;
        this.username = username;
        this.email = email;
        this.timeCreated = timeCreated;
    }

    public User(UserDto userDto) {
        this(userDto.getType(), userDto.getUsername(), userDto.getEmail(), LocalDateTime.now(ZoneOffset.UTC));
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private UserType type = UserType.STANDARD;
    @Enumerated(EnumType.STRING)  // doesn't have any meaning for ADMIN user type
    private PaymentPlan paymentPlan = PaymentPlan.FREE;

    @Nonnull
    private String username;
    @Nonnull
    private String email;
    @Nonnull
    private LocalDateTime timeCreated;
    @Column(columnDefinition = "BINARY")
    private byte[] password = new byte[0];
}