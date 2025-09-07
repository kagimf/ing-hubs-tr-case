package com.ing.hubs.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ing.hubs.exception.CustomConflictException;
import com.ing.hubs.model.Currency;

import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "transactions")
@Table(name = "wallet")
public class Wallet {

  @Id private String id;
  @Version private Long version = 0L;

  @ManyToOne
  @JoinColumn(name = "customer_id", nullable = false)
  @JsonIgnore
  private Customer customer;

  @Column(nullable = false)
  private String walletName;

  @Column(nullable = false)
  private Currency currency;

  @Column(nullable = false)
  private boolean activeForShopping;

  @Column(nullable = false)
  private boolean activeForWithdraw;

  @Column(nullable = false)
  private BigDecimal balance;

  @Column(nullable = false)
  private BigDecimal usableBalance;

  @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Transaction> transactions = new ArrayList<>();

  public void increaseBalance(BigDecimal amount) {

    this.balance = this.balance.add(amount);
    this.version++;
  }

  public void decreaseBalance(BigDecimal amount) {

    if (this.balance.compareTo(amount) < 0) {

      throw new CustomConflictException("Balance is not sufficient!");
    }

    this.balance = this.balance.subtract(amount);
    this.version++;
  }

  public void increaseUsableBalance(BigDecimal amount) {

    this.usableBalance = this.usableBalance.add(amount);
    this.version++;
  }

  public void decreaseUsableBalance(BigDecimal amount) {

    if (this.usableBalance.compareTo(amount) < 0) {

      throw new CustomConflictException("Usable balance is not sufficient!");
    }

    this.usableBalance = this.usableBalance.subtract(amount);
    this.version++;
  }
}
