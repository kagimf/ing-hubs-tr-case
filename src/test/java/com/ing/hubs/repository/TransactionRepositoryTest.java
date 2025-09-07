package com.ing.hubs.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.ing.hubs.entity.Customer;
import com.ing.hubs.entity.Transaction;
import com.ing.hubs.entity.Wallet;
import com.ing.hubs.model.Currency;
import com.ing.hubs.model.OppositePartyType;
import com.ing.hubs.model.TransactionStatus;
import com.ing.hubs.model.TransactionType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

  @Autowired private TransactionRepository transactionRepository;

  @Autowired private WalletRepository walletRepository;

  @Autowired private TestEntityManager entityManager;

  private Customer testCustomer;
  private Wallet testWallet;

  @BeforeEach
  void setUp() {

    testCustomer =
        Customer.builder()
            .id("cust-123")
            .tckn("12345678901")
            .name("John")
            .surname("Doe")
            .password("customerPass123")
            .wallets(new ArrayList<>())
            .build();

    testWallet =
        Wallet.builder()
            .id("wallet-1")
            .customer(testCustomer)
            .walletName("Main Wallet")
            .currency(Currency.USD)
            .activeForShopping(true)
            .activeForWithdraw(true)
            .balance(BigDecimal.valueOf(1000))
            .usableBalance(BigDecimal.valueOf(1000))
            .transactions(new ArrayList<>())
            .build();

    Transaction testTransaction =
        Transaction.builder()
            .id("trans-1")
            .amount(BigDecimal.valueOf(100))
            .type(TransactionType.DEPOSIT)
            .oppositePartyType(OppositePartyType.IBAN)
            .oppositeParty("Bank of America")
            .status(TransactionStatus.APPROVED)
            .wallet(testWallet)
            .build();

    entityManager.persist(testCustomer);
    entityManager.persist(testWallet);
    entityManager.persist(testTransaction);
    entityManager.flush();
  }

  @Test
  void findAllByWalletId_WhenWalletHasTransactions_ShouldReturnTransactions() {

    List<Transaction> transactions = transactionRepository.findAllByWalletId("wallet-1");

    assertThat(transactions).hasSize(1);
    assertThat(transactions.getFirst().getAmount()).isEqualByComparingTo("100");
    assertThat(transactions.getFirst().getType()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(transactions.getFirst().getWallet().getId()).isEqualTo("wallet-1");
  }

  @Test
  void findAllByWalletId_WhenWalletNoTransactions_ShouldReturnEmptyList() {

    Wallet emptyWallet =
        Wallet.builder()
            .id("wallet-2")
            .customer(testCustomer)
            .walletName("Empty Wallet")
            .currency(Currency.USD)
            .activeForShopping(true)
            .activeForWithdraw(true)
            .balance(BigDecimal.ZERO)
            .usableBalance(BigDecimal.ZERO)
            .transactions(new ArrayList<>())
            .build();

    entityManager.persist(emptyWallet);
    entityManager.flush();

    List<Transaction> transactions = transactionRepository.findAllByWalletId("wallet-2");

    assertThat(transactions).isEmpty();
  }

  @Test
  void findAllByWalletId_WhenWalletNotExists_ShouldReturnEmptyList() {

    String nonExistentWalletId = "non-existent-wallet";

    List<Transaction> transactions = transactionRepository.findAllByWalletId(nonExistentWalletId);

    assertThat(transactions).isEmpty();
  }

  @Test
  void findAllByWalletId_WithMultipleTransactions_ShouldReturnAll() {

    Transaction secondTransaction =
        Transaction.builder()
            .id("trans-2")
            .amount(BigDecimal.valueOf(50))
            .type(TransactionType.WITHDRAW)
            .oppositePartyType(OppositePartyType.IBAN)
            .oppositeParty("Bank of America")
            .status(TransactionStatus.PENDING)
            .wallet(testWallet)
            .build();

    entityManager.persist(secondTransaction);
    entityManager.flush();

    List<Transaction> transactions = transactionRepository.findAllByWalletId("wallet-1");

    assertThat(transactions).hasSize(2);
    assertThat(transactions)
        .extracting("amount")
        .containsExactly(BigDecimal.valueOf(100), BigDecimal.valueOf(50));
  }
}
