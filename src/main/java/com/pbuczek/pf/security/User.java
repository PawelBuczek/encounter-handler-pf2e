package com.pbuczek.pf.security;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {

    public User(UserType type, String username, String email) {
        this.type = type;
        this.username = username;
        this.email = email;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private UserType type = UserType.STANDARD;
    @Enumerated(EnumType.STRING)  // doesn't have any meaning for ADMIN user type
    private PaymentPlan paymentPlan = PaymentPlan.FREE;

    private String username;
    private String email;
    @Column(columnDefinition = "BINARY")
    private byte[] password = new byte[0];
}