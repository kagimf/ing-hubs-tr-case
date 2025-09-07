package com.ing.hubs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "admin_user")
public class AdminUser {

  @Id private String id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String password;
}
