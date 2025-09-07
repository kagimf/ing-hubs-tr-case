package com.ing.hubs.dto;

import com.ing.hubs.model.TransactionStatus;

import lombok.Builder;

@Builder
public record TransactionResponseDto(
    String transactionId, TransactionStatus status, String message) {}
