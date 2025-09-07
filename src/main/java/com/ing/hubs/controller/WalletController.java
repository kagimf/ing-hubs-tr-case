package com.ing.hubs.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ing.hubs.dto.*;
import com.ing.hubs.entity.Customer;
import com.ing.hubs.entity.Wallet;
import com.ing.hubs.model.TransactionStatus;
import com.ing.hubs.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/wallet")
@RequiredArgsConstructor
public class WalletController {

  private final WalletService walletService;

  @PostMapping("create")
  @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
  public ResponseEntity<Wallet> createWallet(@RequestBody @Valid CreateWalletDto createWalletDto) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(walletService.createWallet(createWalletDto));
  }

  @GetMapping("{customerId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
  public ResponseEntity<ListWalletDto> getWallets(
      @PathVariable @NotBlank(message = "Customer ID cannot be blank") String customerId) {

    return ResponseEntity.ok().body(walletService.listWallets(customerId));
  }

  @PostMapping("deposit")
  @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
  public ResponseEntity<TransactionResponseDto> makeDeposit(
      @RequestBody @Valid DepositDto depositDto) {

    return ResponseEntity.ok().body(walletService.makeDeposit(depositDto));
  }

  @GetMapping("transactions/{walletId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
  public ResponseEntity<ListTransactionDto> getTransactions(
      @PathVariable @NotBlank(message = "Wallet ID cannot be blank") String walletId) {

    return ResponseEntity.ok().body(walletService.listTransactions(walletId));
  }

  @PostMapping("withdraw")
  @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
  public ResponseEntity<TransactionResponseDto> makeWithdraw(
      @RequestBody @Valid WithdrawDto withdrawDto) {

    return ResponseEntity.ok().body(walletService.makeWithdraw(withdrawDto));
  }

  @PutMapping("transaction/{transactionId}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<TransactionResponseDto> approve(
      @PathVariable @NotBlank(message = "Transaction ID cannot be blank") String transactionId,
      @RequestParam TransactionStatus status) {

    return ResponseEntity.ok().body(walletService.approve(transactionId, status));
  }

  @PostMapping("/create/customer")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<Customer> createCustomer(
      @RequestBody @Valid CreateCustomerDto createCustomerDto) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(walletService.createCustomer(createCustomerDto));
  }
}
