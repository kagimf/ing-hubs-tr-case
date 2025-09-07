package com.ing.hubs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ing.hubs.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

  List<Transaction> findAllByWalletId(String walletId);
}
