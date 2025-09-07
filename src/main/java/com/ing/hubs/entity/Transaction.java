package com.ing.hubs.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ing.hubs.model.OppositePartyType;
import com.ing.hubs.model.TransactionStatus;
import com.ing.hubs.model.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "transaction")
public class Transaction {

  @Id private String id;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private TransactionType type;

  @Column(nullable = false)
  private OppositePartyType oppositePartyType;

  private String oppositeParty;

  @Column(nullable = false)
  private TransactionStatus status;

  @ManyToOne
  @JoinColumn(name = "wallet_id", nullable = false)
  @JsonIgnore
  private Wallet wallet;
}
