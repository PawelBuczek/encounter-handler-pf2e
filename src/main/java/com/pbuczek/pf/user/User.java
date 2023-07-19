package com.pbuczek.pf.user;

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

    public User(UserType type, String username, String email, String password) {
        this.type = type;
        this.username = username;
        this.email = email;
        this.password = password;
        this.timeCreated = LocalDateTime.now(ZoneOffset.UTC);
    }

    public User(UserDto userDto) {
        this(userDto.getType(), userDto.getUsername(), userDto.getEmail(), userDto.getPassword());
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
    @Nonnull
    @Column(columnDefinition = "CHAR(60)")
    private String password;
}