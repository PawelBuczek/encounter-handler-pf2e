package com.pbuczek.pf.user;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static com.pbuczek.pf.security.SecurityHelper.passwordEncoder;

@ResponseBody
@Data
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = passwordEncoder.encode(password);
        this.timeCreated = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        this.passwordLastUpdatedDate = LocalDate.now(ZoneOffset.UTC);
    }

    public User(UserDto userDto) {
        this(userDto.getUsername(), userDto.getEmail(), userDto.getPassword());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Enumerated(EnumType.STRING)
    private UserType type = UserType.STANDARD;
    @Enumerated(EnumType.STRING)  // shouldn't have any meaning for ADMIN user type
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
    @Nonnull
    private LocalDate passwordLastUpdatedDate;
    @Nonnull
    private Boolean locked = false;
    @Nonnull
    private Boolean enabled = false;

    public void refreshPasswordLastUpdatedDate() {
        this.passwordLastUpdatedDate = LocalDate.now(ZoneOffset.UTC);
    }
}