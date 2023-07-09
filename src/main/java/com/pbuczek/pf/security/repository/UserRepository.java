package com.pbuczek.pf.security.repository;

import com.pbuczek.pf.security.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findAllById(Integer id);

    Optional<User> findById(Integer id);

    @Transactional
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = ?1")
    int deleteUser(Integer id);

}