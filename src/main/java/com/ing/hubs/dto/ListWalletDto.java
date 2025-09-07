package com.ing.hubs.dto;

import java.util.List;

import com.ing.hubs.entity.Wallet;

import lombok.Builder;

@Builder
public record ListWalletDto(String customerId, List<Wallet> wallets) {}
