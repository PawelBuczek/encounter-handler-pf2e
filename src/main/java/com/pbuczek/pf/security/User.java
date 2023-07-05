package com.pbuczek.pf.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user")
public class User {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    private UserType type;
    private String username;
    private String email;
    @Column(columnDefinition = "BINARY")
    private byte[] password;
}