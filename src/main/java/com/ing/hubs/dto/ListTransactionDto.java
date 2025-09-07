package com.ing.hubs.dto;

import java.util.List;

import com.ing.hubs.entity.Transaction;

import lombok.Builder;

@Builder
public record ListTransactionDto(String walletId, List<Transaction> transactions) {}
