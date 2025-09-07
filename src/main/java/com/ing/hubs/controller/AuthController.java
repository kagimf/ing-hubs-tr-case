package com.ing.hubs.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ing.hubs.dto.AuthRequestDto;
import com.ing.hubs.dto.AuthResponseDto;
import com.ing.hubs.dto.CreateAdminUserDto;
import com.ing.hubs.entity.AdminUser;
import com.ing.hubs.security.JwtTokenProvider;
import com.ing.hubs.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final WalletService walletService;

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid AuthRequestDto authRequestDto) {

    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                authRequestDto.username(), authRequestDto.password()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    String role =
        authentication.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse("");

    String jwt = jwtTokenProvider.generateToken(authRequestDto.username(), role);

    return ResponseEntity.ok(new AuthResponseDto(jwt, role));
  }

  @PostMapping("/create/admin")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<AdminUser> createAdminUser(
      @RequestBody @Valid CreateAdminUserDto createAdminUserDto) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(walletService.createAdminUser(createAdminUserDto));
  }
}
