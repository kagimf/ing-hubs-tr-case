package com.ing.hubs.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import com.ing.hubs.model.OppositePartyType;

public record DepositDto(
    @NotNull BigDecimal amount,
    @NotNull String walletId,
    @NotNull OppositePartyType source,
    @NotNull String oppositeParty) {}
