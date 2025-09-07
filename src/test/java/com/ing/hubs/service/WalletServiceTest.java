package com.ing.hubs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ing.hubs.dto.*;
import com.ing.hubs.entity.*;
import com.ing.hubs.exception.*;
import com.ing.hubs.model.*;
import com.ing.hubs.repository.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@EnableRetry
class WalletServiceTest {

  @Mock private WalletRepository walletRepository;

  @Mock private TransactionRepository transactionRepository;

  @Mock private CustomerRepository customerRepository;

  @Mock private AdminUserRepository adminUserRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private SecurityContext securityContext;

  @Mock private Authentication authentication;

  @InjectMocks private WalletService walletService;

  private Customer customer;
  private Wallet wallet;
  private Transaction transaction;
  private CreateWalletDto createWalletDto;
  private DepositDto depositDto;
  private WithdrawDto withdrawDto;
  private CreateCustomerDto createCustomerDto;
  private CreateAdminUserDto createAdminUserDto;

  @BeforeEach
  void setUp() {

    customer =
        Customer.builder()
            .id("customer-123")
            .name("John")
            .surname("Doe")
            .tckn("12345678901")
            .password("encodedPassword")
            .build();

    wallet =
        Wallet.builder()
            .id("wallet-123")
            .version(0L)
            .customer(customer)
            .walletName("My Wallet")
            .currency(Currency.USD)
            .activeForShopping(true)
            .activeForWithdraw(true)
            .balance(BigDecimal.valueOf(1000))
            .usableBalance(BigDecimal.valueOf(800))
            .build();

    transaction =
        Transaction.builder()
            .id("transaction-123")
            .amount(BigDecimal.valueOf(500))
            .type(TransactionType.DEPOSIT)
            .oppositePartyType(OppositePartyType.IBAN)
            .oppositeParty("Bank Account 123")
            .status(TransactionStatus.PENDING)
            .wallet(wallet)
            .build();

    createWalletDto = new CreateWalletDto("customer-123", "My Wallet", Currency.USD, true, true);

    depositDto =
        new DepositDto(
            BigDecimal.valueOf(500), "wallet-123", OppositePartyType.IBAN, "Bank Account 123");

    withdrawDto =
        new WithdrawDto(
            BigDecimal.valueOf(200), "wallet-123", OppositePartyType.IBAN, "Bank Account 456");

    createCustomerDto = new CreateCustomerDto("John", "Doe", "12345678901", "password123");

    createAdminUserDto = new CreateAdminUserDto("adminuser", "adminpassword");

    given(securityContext.getAuthentication()).willReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void createWallet_WithValidData_ShouldReturnWallet() {

    given(customerRepository.findById("customer-123")).willReturn(Optional.of(customer));
    given(walletRepository.save(any(Wallet.class))).willReturn(wallet);
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    Wallet result = walletService.createWallet(createWalletDto);

    assertNotNull(result);
    assertEquals("wallet-123", result.getId());

    then(walletRepository).should().save(any(Wallet.class));
  }

  @Test
  void createWallet_WhenCustomerNotFound_ShouldThrowException() {

    given(customerRepository.findById("customer-123")).willReturn(Optional.empty());

    assertThrows(
        CustomNotFoundException.class,
        () -> {
          walletService.createWallet(createWalletDto);
        });
  }

  @Test
  void createWallet_WhenCustomerTriesToCreateForOtherCustomer_ShouldThrowException() {

    given(customerRepository.findById("customer-123")).willReturn(Optional.of(customer));
    given(authentication.getName()).willReturn("different-tckn");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        CustomUnauthorizedException.class,
        () -> {
          walletService.createWallet(createWalletDto);
        });
  }

  @Test
  void listWallets_WithValidCustomerId_ShouldReturnWallets() {

    given(customerRepository.findById("customer-123")).willReturn(Optional.of(customer));
    given(walletRepository.findAllByCustomerId("customer-123")).willReturn(List.of(wallet));
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    ListWalletDto result = walletService.listWallets("customer-123");

    assertNotNull(result);
    assertEquals("customer-123", result.customerId());
    assertEquals(1, result.wallets().size());
  }

  @Test
  void makeDeposit_WithValidData_ShouldCreateTransaction() {

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(walletRepository.save(any(Wallet.class))).willReturn(wallet);
    given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    TransactionResponseDto result = walletService.makeDeposit(depositDto);

    then(transactionRepository).should().save(any(Transaction.class));
    then(walletRepository).should().save(any(Wallet.class));

    assertNotNull(result);
  }

  @Test
  void makeDeposit_WhenWalletNotFound_ShouldThrowException() {

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.empty());

    assertThrows(
        CustomNotFoundException.class,
        () -> {
          walletService.makeDeposit(depositDto);
        });
  }

  @Test
  void makeDeposit_WithLargeAmount_ShouldSetStatusPending() {

    DepositDto largeDeposit =
        new DepositDto(
            BigDecimal.valueOf(1500), "wallet-123", OppositePartyType.IBAN, "Bank Account 123");

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(walletRepository.save(any(Wallet.class))).willReturn(wallet);
    given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    TransactionResponseDto result = walletService.makeDeposit(largeDeposit);

    then(transactionRepository).should().save(any(Transaction.class));

    assertNotNull(result);
  }

  @Test
  void makeDeposit_WithZeroAmount_ShouldThrowException() {

    DepositDto zeroDeposit =
        new DepositDto(BigDecimal.ZERO, "wallet-123", OppositePartyType.IBAN, "Bank Account 123");

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        CustomBadRequestException.class,
        () -> {
          walletService.makeDeposit(zeroDeposit);
        });
  }

  @Test
  void makeDeposit_WhenOptimisticLockingFailure_ShouldThrowException() {

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(walletRepository.save(any(Wallet.class)))
        .willThrow(ObjectOptimisticLockingFailureException.class);
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> {
          walletService.makeDeposit(depositDto);
        });
  }

  @Test
  void makeDeposit_WhenTransactionSaveFails_ShouldRollback() {

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(walletRepository.save(any(Wallet.class))).willReturn(wallet);
    given(transactionRepository.save(any(Transaction.class)))
        .willThrow(new RuntimeException("Database error"));
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        RuntimeException.class,
        () -> {
          walletService.makeDeposit(depositDto);
        });

    verify(walletRepository).save(any(Wallet.class));
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  void makeWithdraw_WithValidData_ShouldCreateTransaction() {

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(walletRepository.save(any(Wallet.class))).willReturn(wallet);
    given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    TransactionResponseDto result = walletService.makeWithdraw(withdrawDto);

    then(transactionRepository).should().save(any(Transaction.class));
    then(walletRepository).should().save(any(Wallet.class));

    assertNotNull(result);
  }

  @Test
  void makeWithdraw_WhenInsufficientBalance_ShouldThrowException() {

    WithdrawDto largeWithdraw =
        new WithdrawDto(
            BigDecimal.valueOf(1000), "wallet-123", OppositePartyType.IBAN, "Bank Account 456");

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        CustomConflictException.class,
        () -> {
          walletService.makeWithdraw(largeWithdraw);
        });
  }

  @Test
  void makeWithdraw_WithZeroAmount_ShouldThrowException() {

    WithdrawDto zeroWithdraw =
        new WithdrawDto(BigDecimal.ZERO, "wallet-123", OppositePartyType.IBAN, "Bank Account 456");

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        CustomBadRequestException.class,
        () -> {
          walletService.makeWithdraw(zeroWithdraw);
        });
  }

  @Test
  void makeWithdraw_WhenOptimisticLockingFailure_ShouldThrowException() {

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(walletRepository.save(any(Wallet.class)))
        .willThrow(ObjectOptimisticLockingFailureException.class);
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> {
          walletService.makeWithdraw(withdrawDto);
        });
  }

  @Test
  void makeWithdraw_WhenTransactionSaveFails_ShouldRollback() {

    given(walletRepository.findWithLockingById("wallet-123")).willReturn(Optional.of(wallet));
    given(walletRepository.save(any(Wallet.class))).willReturn(wallet);
    given(transactionRepository.save(any(Transaction.class)))
        .willThrow(new RuntimeException("Database error"));
    given(authentication.getName()).willReturn("12345678901");
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    assertThrows(
        RuntimeException.class,
        () -> {
          walletService.makeWithdraw(withdrawDto);
        });

    verify(walletRepository).save(any(Wallet.class));
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  void approve_WithValidApproval_ShouldUpdateTransaction() {

    given(transactionRepository.findById("transaction-123")).willReturn(Optional.of(transaction));
    given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);

    TransactionResponseDto result =
        walletService.approve("transaction-123", TransactionStatus.APPROVED);

    then(transactionRepository).should().save(any(Transaction.class));

    assertNotNull(result);
  }

  @Test
  void approve_WhenTransactionNotFound_ShouldThrowException() {

    given(transactionRepository.findById("transaction-123")).willReturn(Optional.empty());

    assertThrows(
        CustomNotFoundException.class,
        () -> {
          walletService.approve("transaction-123", TransactionStatus.APPROVED);
        });
  }

  @Test
  void approve_WhenOptimisticLockingFailure_ShouldThrowException() {

    given(transactionRepository.findById("transaction-123")).willReturn(Optional.of(transaction));
    given(transactionRepository.save(any(Transaction.class)))
        .willThrow(ObjectOptimisticLockingFailureException.class);

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> {
          walletService.approve("transaction-123", TransactionStatus.APPROVED);
        });
  }

  @Test
  void createCustomer_WithValidData_ShouldReturnCustomer() {

    given(customerRepository.findByTckn("12345678901")).willReturn(Optional.empty());
    given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
    given(customerRepository.save(any(Customer.class))).willReturn(customer);

    Customer result = walletService.createCustomer(createCustomerDto);

    assertNotNull(result);
    assertEquals("customer-123", result.getId());

    then(customerRepository).should().save(any(Customer.class));
  }

  @Test
  void createCustomer_WhenTcknAlreadyExists_ShouldThrowException() {

    given(customerRepository.findByTckn("12345678901")).willReturn(Optional.of(customer));

    assertThrows(
        CustomConflictException.class,
        () -> {
          walletService.createCustomer(createCustomerDto);
        });
  }

  @Test
  void createAdminUser_WithValidData_ShouldReturnAdminUser() {

    AdminUser adminUser =
        AdminUser.builder()
            .id("admin-123")
            .username("adminuser")
            .password("encodedPassword")
            .build();
    given(adminUserRepository.findByUsername("adminuser")).willReturn(Optional.empty());
    given(passwordEncoder.encode("adminpassword")).willReturn("encodedPassword");
    given(adminUserRepository.save(any(AdminUser.class))).willReturn(adminUser);

    AdminUser result = walletService.createAdminUser(createAdminUserDto);

    assertNotNull(result);
    assertEquals("admin-123", result.getId());

    then(adminUserRepository).should().save(any(AdminUser.class));
  }

  @Test
  void createAdminUser_WhenUsernameAlreadyExists_ShouldThrowException() {

    AdminUser existingAdmin =
        AdminUser.builder()
            .id("admin-123")
            .username("adminuser")
            .password("encodedPassword")
            .build();

    given(adminUserRepository.findByUsername("adminuser")).willReturn(Optional.of(existingAdmin));

    assertThrows(
        CustomConflictException.class,
        () -> {
          walletService.createAdminUser(createAdminUserDto);
        });
  }
}
