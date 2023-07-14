package com.pbuczek.pf.security.repository;

import com.pbuczek.pf.security.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findById(Integer id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Transactional
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = ?1")
    int deleteUser(Integer id);

}