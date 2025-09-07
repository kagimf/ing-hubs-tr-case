package com.ing.hubs.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.ing.hubs.entity.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {

  @Lock(LockModeType.OPTIMISTIC)
  Optional<Wallet> findWithLockingById(String id);

  List<Wallet> findAllByCustomerId(String customerId);
}
