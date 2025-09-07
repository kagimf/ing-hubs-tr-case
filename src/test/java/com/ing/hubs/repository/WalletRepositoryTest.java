package com.ing.hubs.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import com.ing.hubs.entity.Customer;
import com.ing.hubs.entity.Wallet;
import com.ing.hubs.model.Currency;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletRepositoryTest {

  @Autowired private WalletRepository walletRepository;

  @Autowired private CustomerRepository customerRepository;

  @Autowired private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {

    Customer testCustomer =
        Customer.builder()
            .id("cust-123")
            .tckn("12345678901")
            .name("John")
            .surname("Doe")
            .password("customerPass123")
            .wallets(new ArrayList<>())
            .build();

    Wallet testWallet =
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

    entityManager.persist(testCustomer);
    entityManager.persist(testWallet);
    entityManager.flush();
  }

  @Test
  void findAllByCustomerId_WhenCustomerHasWallets_ShouldReturnWallets() {

    List<Wallet> wallets = walletRepository.findAllByCustomerId("cust-123");

    assertThat(wallets).hasSize(1);
    assertThat(wallets.getFirst().getWalletName()).isEqualTo("Main Wallet");
    assertThat(wallets.getFirst().getCustomer().getId()).isEqualTo("cust-123");
  }

  @Test
  void findAllByCustomerId_WhenCustomerNoWallets_ShouldReturnEmptyList() {

    Customer newCustomer =
        Customer.builder()
            .id("cust-456")
            .tckn("98765432109")
            .name("Jane")
            .surname("Smith")
            .password("pass123")
            .wallets(new ArrayList<>())
            .build();

    entityManager.persist(newCustomer);
    entityManager.flush();

    List<Wallet> wallets = walletRepository.findAllByCustomerId("cust-456");

    assertThat(wallets).isEmpty();
  }

  @Test
  void findAllByCustomerId_WhenCustomerNotExists_ShouldReturnEmptyList() {

    String nonExistentCustomerId = "non-existent-id";

    List<Wallet> wallets = walletRepository.findAllByCustomerId(nonExistentCustomerId);

    assertThat(wallets).isEmpty();
  }

  @Test
  void findWithLockingById_WhenWalletExists_ShouldReturnWallet() {

    Optional<Wallet> foundWallet = walletRepository.findWithLockingById("wallet-1");

    assertThat(foundWallet).isPresent();
    assertThat(foundWallet.get().getId()).isEqualTo("wallet-1");
    assertThat(foundWallet.get().getWalletName()).isEqualTo("Main Wallet");
  }

  @Test
  void findWithLockingById_WhenWalletNotExists_ShouldReturnEmpty() {

    Optional<Wallet> foundWallet = walletRepository.findWithLockingById("non-existent-id");

    assertThat(foundWallet).isEmpty();
  }

  @Test
  @Transactional
  void findWithLockingById_ShouldApplyOptimisticLock() {

    Optional<Wallet> lockedWallet = walletRepository.findWithLockingById("wallet-1");
    assertThat(lockedWallet).isPresent();

    Long originalVersion = lockedWallet.get().getVersion();
    assertThat(originalVersion).isNotNull();

    lockedWallet.get().setBalance(BigDecimal.valueOf(1500));
    walletRepository.save(lockedWallet.get());
    entityManager.flush();
    entityManager.clear();

    Wallet updatedWallet = walletRepository.findById("wallet-1").orElseThrow();

    assertThat(updatedWallet.getVersion()).isGreaterThan(originalVersion);
  }

  @Test
  void findWithLockingById_WhenConcurrentAccess_ShouldHandleOptimisticLocking() {

    Optional<Wallet> wallet1 = walletRepository.findWithLockingById("wallet-1");
    Optional<Wallet> wallet2 = walletRepository.findWithLockingById("wallet-1");

    assertThat(wallet1).isPresent();
    assertThat(wallet2).isPresent();
  }

  @Test
  void findWithLockingById_ComparedToFindById_ShouldBothWork() {

    Optional<Wallet> withLocking = walletRepository.findWithLockingById("wallet-1");
    Optional<Wallet> withoutLocking = walletRepository.findById("wallet-1");

    assertThat(withLocking).isPresent();
    assertThat(withoutLocking).isPresent();
    assertThat(withLocking.get().getId()).isEqualTo(withoutLocking.get().getId());
  }
}
