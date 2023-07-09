package com.pbuczek.pf.security.repository;

import com.pbuczek.pf.security.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findAllById(Integer id);
}