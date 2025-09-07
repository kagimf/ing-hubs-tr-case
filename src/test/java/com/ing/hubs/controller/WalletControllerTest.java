package com.ing.hubs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ing.hubs.dto.*;
import com.ing.hubs.entity.Customer;
import com.ing.hubs.entity.Transaction;
import com.ing.hubs.entity.Wallet;
import com.ing.hubs.exception.CustomConflictException;
import com.ing.hubs.exception.CustomNotFoundException;
import com.ing.hubs.exception.CustomUnauthorizedException;
import com.ing.hubs.model.Currency;
import com.ing.hubs.model.OppositePartyType;
import com.ing.hubs.model.TransactionStatus;
import com.ing.hubs.model.TransactionType;
import com.ing.hubs.service.WalletService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WalletControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WalletService walletService;

  private ObjectMapper objectMapper;
  private CreateWalletDto createWalletDto;
  private DepositDto depositDto;
  private WithdrawDto withdrawDto;
  private CreateCustomerDto createCustomerDto;
  private Wallet wallet;
  private Customer customer;
  private Transaction transaction;
  private TransactionResponseDto transactionResponseDto;

  @BeforeEach
  void setUp() {

    objectMapper = JsonMapper.builder().findAndAddModules().build();

    createWalletDto = new CreateWalletDto("customer-123", "My Wallet", Currency.USD, true, true);

    depositDto =
        new DepositDto(
            BigDecimal.valueOf(500), "wallet-123", OppositePartyType.IBAN, "Bank Account 123");

    withdrawDto =
        new WithdrawDto(
            BigDecimal.valueOf(200), "wallet-123", OppositePartyType.PAYMENT, "Bank Account 456");

    createCustomerDto = new CreateCustomerDto("John", "Doe", "12345678901", "password123");

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
            .status(TransactionStatus.APPROVED)
            .wallet(wallet)
            .build();

    transactionResponseDto =
        TransactionResponseDto.builder()
            .transactionId("00d4334d-1e42-429f-9f44-17f19bee8e8c")
            .status(TransactionStatus.APPROVED)
            .message("Transaction done!")
            .build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWallet_WithAdminRole_ShouldReturnCreated() throws Exception {

    given(walletService.createWallet(any(CreateWalletDto.class))).willReturn(wallet);

    mockMvc
        .perform(
            post("/api/wallet/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWalletDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("wallet-123"))
        .andExpect(jsonPath("$.walletName").value("My Wallet"));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void createWallet_WithCustomerRole_ShouldReturnCreated() throws Exception {

    given(walletService.createWallet(any(CreateWalletDto.class))).willReturn(wallet);

    mockMvc
        .perform(
            post("/api/wallet/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWalletDto)))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(roles = "USER")
  void createWallet_WithUnauthorizedRole_ShouldReturnForbidden() throws Exception {

    mockMvc
        .perform(
            post("/api/wallet/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWalletDto)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createWallet_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {

    mockMvc
        .perform(
            post("/api/wallet/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWalletDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWallet_WithInvalidDto_ShouldReturnBadRequest() throws Exception {

    CreateWalletDto invalidDto = new CreateWalletDto(null, null, null, false, false);

    mockMvc
        .perform(
            post("/api/wallet/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listWallets_WithAdminRole_ShouldReturnOk() throws Exception {

    ListWalletDto listWalletDto =
        ListWalletDto.builder().customerId("customer-123").wallets(List.of(wallet)).build();

    given(walletService.listWallets(anyString())).willReturn(listWalletDto);

    mockMvc
        .perform(get("/api/wallet/customer-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerId").value("customer-123"))
        .andExpect(jsonPath("$.wallets").isArray())
        .andExpect(jsonPath("$.wallets[0].id").value("wallet-123"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listWallets_WhenCustomerNotFound_ShouldHandleException() throws Exception {

    given(walletService.listWallets(anyString()))
        .willThrow(new CustomNotFoundException("Customer not found!"));

    mockMvc.perform(get("/api/wallet/nonexistent-customer")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void makeDeposit_WithValidRequest_ShouldReturnOk() throws Exception {

    given(walletService.makeDeposit(any(DepositDto.class))).willReturn(transactionResponseDto);

    mockMvc
        .perform(
            post("/api/wallet/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("00d4334d-1e42-429f-9f44-17f19bee8e8c"))
        .andExpect(jsonPath("$.status").value(TransactionStatus.APPROVED.name()))
        .andExpect(jsonPath("$.message").value("Transaction done!"));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void makeDeposit_WithInvalidDto_ShouldReturnBadRequest() throws Exception {

    DepositDto invalidDto = new DepositDto(null, null, null, null);

    mockMvc
        .perform(
            post("/api/wallet/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void makeDeposit_WhenWalletNotFound_ShouldHandleException() throws Exception {

    willThrow(new CustomNotFoundException("Wallet not found!"))
        .given(walletService)
        .makeDeposit(any(DepositDto.class));

    mockMvc
        .perform(
            post("/api/wallet/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositDto)))
        .andExpect(status().isNotFound());
  }

  // GET TRANSACTIONS TESTS
  @Test
  @WithMockUser(roles = "ADMIN")
  void listTransactions_ShouldReturnOk() throws Exception {

    ListTransactionDto listTransactionDto =
        ListTransactionDto.builder()
            .walletId("wallet-123")
            .transactions(List.of(transaction))
            .build();

    given(walletService.listTransactions(anyString())).willReturn(listTransactionDto);

    mockMvc
        .perform(get("/api/wallet/transactions/wallet-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.walletId").value("wallet-123"))
        .andExpect(jsonPath("$.transactions").isArray())
        .andExpect(jsonPath("$.transactions[0].id").value("transaction-123"));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void makeWithdraw_WithValidRequest_ShouldReturnOk() throws Exception {

    given(walletService.makeWithdraw(any(WithdrawDto.class))).willReturn(transactionResponseDto);

    mockMvc
        .perform(
            post("/api/wallet/withdraw")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("00d4334d-1e42-429f-9f44-17f19bee8e8c"))
        .andExpect(jsonPath("$.status").value(TransactionStatus.APPROVED.name()))
        .andExpect(jsonPath("$.message").value("Transaction done!"));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void makeWithdraw_WhenInsufficientBalance_ShouldHandleException() throws Exception {

    willThrow(new CustomNotFoundException("Usable balance is not sufficient!"))
        .given(walletService)
        .makeWithdraw(any(WithdrawDto.class));

    mockMvc
        .perform(
            post("/api/wallet/withdraw")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void approveTransaction_WithAdminRole_ShouldReturnOk() throws Exception {

    given(walletService.approve(anyString(), any(TransactionStatus.class)))
        .willReturn(transactionResponseDto);

    mockMvc
        .perform(
            put("/api/wallet/transaction/transaction-123").with(csrf()).param("status", "APPROVED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("00d4334d-1e42-429f-9f44-17f19bee8e8c"))
        .andExpect(jsonPath("$.status").value(TransactionStatus.APPROVED.name()))
        .andExpect(jsonPath("$.message").value("Transaction done!"));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void approveTransaction_WithCustomerRole_ShouldReturnForbidden() throws Exception {

    mockMvc
        .perform(
            put("/api/wallet/transaction/transaction-123").with(csrf()).param("status", "APPROVED"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void approveTransaction_WhenTransactionNotFound_ShouldHandleException() throws Exception {

    willThrow(new CustomNotFoundException("Transaction not found!"))
        .given(walletService)
        .approve(anyString(), any(TransactionStatus.class));

    mockMvc
        .perform(
            put("/api/wallet/transaction/nonexistent-transaction")
                .with(csrf())
                .param("status", "APPROVED"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createCustomer_WithAdminRole_ShouldReturnCreated() throws Exception {

    given(walletService.createCustomer(any(CreateCustomerDto.class))).willReturn(customer);

    mockMvc
        .perform(
            post("/api/wallet/create/customer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCustomerDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("customer-123"))
        .andExpect(jsonPath("$.name").value("John"));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void createCustomer_WithCustomerRole_ShouldReturnForbidden() throws Exception {

    mockMvc
        .perform(
            post("/api/wallet/create/customer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCustomerDto)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createCustomer_WhenTcknAlreadyExists_ShouldHandleException() throws Exception {

    given(walletService.createCustomer(any(CreateCustomerDto.class)))
        .willThrow(new CustomConflictException("TCKN already exists"));

    mockMvc
        .perform(
            post("/api/wallet/create/customer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCustomerDto)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void whenCustomerTriesToAccessOtherCustomerData_ShouldHandleException() throws Exception {

    given(walletService.listWallets(anyString()))
        .willThrow(new CustomUnauthorizedException("You can only access your own wallets!"));

    mockMvc.perform(get("/api/wallet/other-customer-456")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void whenInvalidJson_ShouldReturnBadRequest() throws Exception {

    String invalidJson = "{ invalid: json }";

    mockMvc
        .perform(
            post("/api/wallet/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @EnumSource(TransactionStatus.class)
  @WithMockUser(roles = "ADMIN")
  void approveTransaction_WithDifferentStatusValues_ShouldWork(TransactionStatus status)
      throws Exception {

    given(walletService.approve(anyString(), any(TransactionStatus.class)))
        .willReturn(transactionResponseDto);

    mockMvc
        .perform(
            put("/api/wallet/transaction/transaction-123")
                .with(csrf())
                .param("status", status.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("00d4334d-1e42-429f-9f44-17f19bee8e8c"))
        .andExpect(jsonPath("$.status").value(TransactionStatus.APPROVED.name()))
        .andExpect(jsonPath("$.message").value("Transaction done!"));
  }
}
