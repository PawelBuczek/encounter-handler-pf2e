package com.pbuczek.pf.user;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findById(Integer id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u.id FROM User u WHERE u.username = ?1")
    Integer getIdByUsername(String username);

    @Transactional
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = ?1")
    int deleteUserByUserId(Integer id);
}