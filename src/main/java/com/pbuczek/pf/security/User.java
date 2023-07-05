package com.pbuczek.pf.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

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

    @ManyToMany
    @JoinTable(
            name = "users_user_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Collection<UserRole> roles;
}