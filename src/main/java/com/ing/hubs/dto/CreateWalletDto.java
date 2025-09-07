package com.ing.hubs.dto;

import jakarta.validation.constraints.NotNull;

import com.ing.hubs.model.Currency;

import lombok.Builder;

@Builder
public record CreateWalletDto(
    @NotNull String customerId,
    @NotNull String walletName,
    @NotNull Currency currency,
    @NotNull boolean activeForShopping,
    @NotNull boolean activeForWithdraw) {}
