package com.ing.hubs.entity;

import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "wallets")
@Table(name = "customer")
public class Customer {

  @Id private String id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String surname;

  @Column(unique = true, nullable = false, length = 11)
  private String tckn;

  @Column(nullable = false)
  private String password;

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Wallet> wallets;
}
