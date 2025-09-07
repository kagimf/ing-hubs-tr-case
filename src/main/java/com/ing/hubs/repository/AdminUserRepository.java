package com.ing.hubs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ing.hubs.entity.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

  Optional<AdminUser> findByUsername(String username);
}
