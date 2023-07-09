package com.pbuczek.pf.security.repository;

import com.pbuczek.pf.security.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findAllById(Integer id);

    Optional<User> findById(Integer id);
}