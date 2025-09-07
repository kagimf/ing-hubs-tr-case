package com.ing.hubs.dto;

import jakarta.validation.constraints.NotNull;

public record CreateCustomerDto(
    @NotNull String name,
    @NotNull String surname,
    @NotNull String tckn,
    @NotNull String password) {}
