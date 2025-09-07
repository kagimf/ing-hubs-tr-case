package com.ing.hubs.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import com.ing.hubs.model.OppositePartyType;

public record WithdrawDto(
    @NotNull BigDecimal amount,
    @NotNull String walletId,
    @NotNull OppositePartyType destination,
    @NotNull String oppositeParty) {}
