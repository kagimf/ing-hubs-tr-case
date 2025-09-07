package com.ing.hubs.dto;

import lombok.Builder;

@Builder
public record AuthResponseDto(String token, String role) {}
