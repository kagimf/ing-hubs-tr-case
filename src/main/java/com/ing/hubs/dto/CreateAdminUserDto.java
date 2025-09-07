package com.ing.hubs.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAdminUserDto(
    @NotBlank(message = "Username cannot be blank") String username,
    @NotBlank(message = "Password cannot be blank") String password) {}
