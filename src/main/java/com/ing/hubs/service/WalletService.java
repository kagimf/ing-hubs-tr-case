package com.ing.hubs.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ing.hubs.dto.*;
import com.ing.hubs.entity.AdminUser;
import com.ing.hubs.entity.Customer;
import com.ing.hubs.entity.Transaction;
import com.ing.hubs.entity.Wallet;
import com.ing.hubs.exception.*;
import com.ing.hubs.model.TransactionStatus;
import com.ing.hubs.model.TransactionType;
import com.ing.hubs.repository.AdminUserRepository;
import com.ing.hubs.repository.CustomerRepository;
import com.ing.hubs.repository.TransactionRepository;
import com.ing.hubs.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

  private static final String WALLET_NOT_FOUND = "Wallet not found!";
  private static final BigDecimal LARGE_TRANSACTION_LIMIT = new BigDecimal("1000");
  private static final int MAX_RETRY_ATTEMPTS = 3;

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final CustomerRepository customerRepository;
  private final AdminUserRepository adminUserRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public Wallet createWallet(CreateWalletDto createWalletDto) {

    Customer customer =
        customerRepository
            .findById(createWalletDto.customerId())
            .orElseThrow(() -> new CustomNotFoundException("Customer not found!"));

    if (isCurrentUserCustomer() && !customer.getTckn().equals(getCurrentUserTckn())) {

      throw new CustomUnauthorizedException("You can only create your own wallets!");
    }

    Wallet wallet =
        Wallet.builder()
            .id(UUID.randomUUID().toString())
            .customer(customer)
            .walletName(createWalletDto.walletName())
            .currency(createWalletDto.currency())
            .activeForShopping(createWalletDto.activeForShopping())
            .activeForWithdraw(createWalletDto.activeForWithdraw())
            .balance(BigDecimal.ZERO)
            .usableBalance(BigDecimal.ZERO)
            .build();

    return walletRepository.save(wallet);
  }

  @Transactional(readOnly = true)
  public ListWalletDto listWallets(String customerId) {

    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new CustomNotFoundException("Customer not found!"));

    if (isCurrentUserCustomer() && !customer.getTckn().equals(getCurrentUserTckn())) {

      throw new CustomUnauthorizedException("You can only access your own wallets!");
    }

    List<Wallet> walletList = walletRepository.findAllByCustomerId(customerId);

    return ListWalletDto.builder().customerId(customerId).wallets(walletList).build();
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = ObjectOptimisticLockingFailureException.class,
      maxAttempts = MAX_RETRY_ATTEMPTS,
      backoff = @Backoff(delay = 100))
  public TransactionResponseDto makeDeposit(DepositDto depositDto) {

    Wallet wallet =
        walletRepository
            .findWithLockingById(depositDto.walletId())
            .orElseThrow(() -> new CustomNotFoundException(WALLET_NOT_FOUND));

    if (depositDto.amount().compareTo(BigDecimal.ZERO) <= 0) {

      throw new CustomBadRequestException("Deposit amount must be greater than zero!");
    }

    if (isCurrentUserCustomer() && !wallet.getCustomer().getTckn().equals(getCurrentUserTckn())) {

      throw new CustomUnauthorizedException("You can only deposit to your own wallets!");
    }

    Transaction.TransactionBuilder transactionBuilder =
        Transaction.builder()
            .id(UUID.randomUUID().toString())
            .amount(depositDto.amount())
            .type(TransactionType.DEPOSIT)
            .oppositePartyType(depositDto.source())
            .oppositeParty(depositDto.oppositeParty());

    wallet.increaseBalance(depositDto.amount());

    if (depositDto.amount().compareTo(LARGE_TRANSACTION_LIMIT) > 0) {
      transactionBuilder.status(TransactionStatus.PENDING);
    } else {
      transactionBuilder.status(TransactionStatus.APPROVED);
      wallet.increaseUsableBalance(depositDto.amount());
    }

    Transaction transaction = transactionBuilder.wallet(wallet).build();

    walletRepository.save(wallet);
    transactionRepository.save(transaction);

    return TransactionResponseDto.builder()
        .transactionId(transaction.getId())
        .status(transaction.getStatus())
        .message("Deposit transaction created successfully!")
        .build();
  }

  @Transactional(readOnly = true)
  public ListTransactionDto listTransactions(String walletId) {

    Wallet wallet =
        walletRepository
            .findWithLockingById(walletId)
            .orElseThrow(() -> new CustomNotFoundException(WALLET_NOT_FOUND));

    if (isCurrentUserCustomer() && !wallet.getCustomer().getTckn().equals(getCurrentUserTckn())) {

      throw new CustomUnauthorizedException("You can only list transactions of your own wallets!");
    }

    List<Transaction> transactionList = transactionRepository.findAllByWalletId(walletId);

    return ListTransactionDto.builder().walletId(walletId).transactions(transactionList).build();
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = ObjectOptimisticLockingFailureException.class,
      maxAttempts = MAX_RETRY_ATTEMPTS,
      backoff = @Backoff(delay = 100))
  public TransactionResponseDto makeWithdraw(WithdrawDto withdrawDto) {

    Wallet wallet =
        walletRepository
            .findWithLockingById(withdrawDto.walletId())
            .orElseThrow(() -> new CustomNotFoundException(WALLET_NOT_FOUND));

    if (withdrawDto.amount().compareTo(BigDecimal.ZERO) <= 0) {

      throw new CustomBadRequestException("Withdraw amount must be greater than zero!");
    }

    if (isCurrentUserCustomer() && !wallet.getCustomer().getTckn().equals(getCurrentUserTckn())) {

      throw new CustomUnauthorizedException("You can only withdraw from your own wallets!");
    }

    if (!wallet.isActiveForWithdraw() || !wallet.isActiveForShopping()) {

      throw new CustomConflictException("Wallet is not active!");
    }

    Transaction.TransactionBuilder transactionBuilder =
        Transaction.builder()
            .id(UUID.randomUUID().toString())
            .amount(withdrawDto.amount())
            .type(TransactionType.WITHDRAW)
            .oppositePartyType(withdrawDto.destination())
            .oppositeParty(withdrawDto.oppositeParty());

    wallet.decreaseUsableBalance(withdrawDto.amount());

    if (withdrawDto.amount().compareTo(LARGE_TRANSACTION_LIMIT) > 0) {
      transactionBuilder.status(TransactionStatus.PENDING);
    } else {
      transactionBuilder.status(TransactionStatus.APPROVED);
      wallet.decreaseBalance(withdrawDto.amount());
    }

    Transaction transaction = transactionBuilder.wallet(wallet).build();

    walletRepository.save(wallet);
    transactionRepository.save(transaction);

    return TransactionResponseDto.builder()
        .transactionId(transaction.getId())
        .status(transaction.getStatus())
        .message("Withdraw transaction created successfully!")
        .build();
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = ObjectOptimisticLockingFailureException.class,
      maxAttempts = MAX_RETRY_ATTEMPTS,
      backoff = @Backoff(delay = 100))
  public TransactionResponseDto approve(String transactionId, TransactionStatus status) {

    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new CustomNotFoundException("Transaction not found!"));

    Wallet wallet = transaction.getWallet();

    if (!transaction.getStatus().equals(TransactionStatus.PENDING)) {

      throw new CustomConflictException("Transaction processed already!");
    }

    if (status.equals(TransactionStatus.APPROVED)) {
      if (transaction.getType().equals(TransactionType.DEPOSIT)) {
        wallet.increaseUsableBalance(transaction.getAmount());
        transaction.setStatus(TransactionStatus.APPROVED);
      } else {
        wallet.decreaseBalance(transaction.getAmount());
        transaction.setStatus(TransactionStatus.APPROVED);
      }
    } else {
      if (transaction.getType().equals(TransactionType.WITHDRAW)) {
        wallet.increaseUsableBalance(transaction.getAmount());
        transaction.setStatus(TransactionStatus.DENIED);
      } else {
        wallet.decreaseBalance(transaction.getAmount());
        transaction.setStatus(TransactionStatus.DENIED);
      }
    }

    transaction.setWallet(wallet);

    transactionRepository.save(transaction);

    return TransactionResponseDto.builder()
        .transactionId(transaction.getId())
        .status(transaction.getStatus())
        .message("Transaction processed successfully!")
        .build();
  }

  @Transactional
  public Customer createCustomer(CreateCustomerDto createCustomerDto) {

    if (customerRepository.findByTckn(createCustomerDto.tckn()).isPresent()) {

      throw new CustomConflictException("TCKN already exists");
    }

    Customer customer =
        Customer.builder()
            .id(UUID.randomUUID().toString())
            .name(createCustomerDto.name())
            .surname(createCustomerDto.surname())
            .tckn(createCustomerDto.tckn())
            .password(passwordEncoder.encode(createCustomerDto.password()))
            .build();

    return customerRepository.save(customer);
  }

  @Transactional
  public AdminUser createAdminUser(CreateAdminUserDto createAdminUserDto) {

    if (adminUserRepository.findByUsername(createAdminUserDto.username()).isPresent()) {

      throw new CustomConflictException("Username already exists");
    }

    AdminUser adminUser =
        AdminUser.builder()
            .id(UUID.randomUUID().toString())
            .username(createAdminUserDto.username())
            .password(passwordEncoder.encode(createAdminUserDto.password()))
            .build();

    return adminUserRepository.save(adminUser);
  }

  private String getCurrentUserTckn() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    return authentication.getName();
  }

  private boolean isCurrentUserCustomer() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    return authentication.getAuthorities().stream()
        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_CUSTOMER"));
  }
}
