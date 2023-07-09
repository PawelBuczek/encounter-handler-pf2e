package com.pbuczek.pf.security.repository;

import com.pbuczek.pf.security.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findAllById(Integer id);
}