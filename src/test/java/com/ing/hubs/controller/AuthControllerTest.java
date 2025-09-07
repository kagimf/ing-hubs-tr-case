package com.ing.hubs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ing.hubs.dto.AuthRequestDto;
import com.ing.hubs.dto.CreateAdminUserDto;
import com.ing.hubs.entity.AdminUser;
import com.ing.hubs.exception.CustomConflictException;
import com.ing.hubs.security.JwtTokenProvider;
import com.ing.hubs.service.WalletService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthenticationManager authenticationManager;

  @MockitoBean private JwtTokenProvider jwtTokenProvider;

  @MockitoBean private WalletService walletService;

  private ObjectMapper objectMapper;
  private AuthRequestDto authRequestDto;
  private CreateAdminUserDto createAdminUserDto;
  private AdminUser adminUser;

  @BeforeEach
  void setUp() {

    objectMapper = JsonMapper.builder().findAndAddModules().build();

    authRequestDto = new AuthRequestDto("testuser", "password123");
    createAdminUserDto = new CreateAdminUserDto("adminuser", "adminpassword");

    adminUser =
        AdminUser.builder()
            .id(UUID.randomUUID().toString())
            .username("adminuser")
            .password("encodedPassword")
            .build();
  }

  @Test
  void login_WithValidCredentials_ShouldReturnTokenAndRole() throws Exception {

    Authentication authentication = mock(Authentication.class);

    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willReturn(authentication);
    given(authentication.getAuthorities())
        .willAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    given(jwtTokenProvider.generateToken(anyString(), anyString())).willReturn("jwt-token-123");

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token-123"))
        .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
  }

  @Test
  void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {

    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willThrow(new BadCredentialsException("Invalid credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequestDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_WithEmptyUsername_ShouldReturnBadRequest() throws Exception {

    AuthRequestDto invalidRequest = new AuthRequestDto("", "password123");

    willThrow(new BadCredentialsException("Invalid credentials"))
        .given(authenticationManager)
        .authenticate(any(UsernamePasswordAuthenticationToken.class));

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(authenticationManager, never())
        .authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void login_WithEmptyPassword_ShouldReturnBadRequest() throws Exception {

    AuthRequestDto invalidRequest = new AuthRequestDto("testuser", "");

    willThrow(new BadCredentialsException("Invalid credentials"))
        .given(authenticationManager)
        .authenticate(any(UsernamePasswordAuthenticationToken.class));

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(authenticationManager, never())
        .authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void login_WithNullCredentials_ShouldReturnBadRequest() throws Exception {

    AuthRequestDto invalidRequest = new AuthRequestDto(null, null);

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(authenticationManager, never())
        .authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createAdminUser_WithAdminRole_ShouldReturnCreated() throws Exception {

    given(walletService.createAdminUser(any(CreateAdminUserDto.class))).willReturn(adminUser);

    mockMvc
        .perform(
            post("/api/auth/create/admin")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdminUserDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.username").value("adminuser"));
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void createAdminUser_WithCustomerRole_ShouldReturnForbidden() throws Exception {

    mockMvc
        .perform(
            post("/api/auth/create/admin")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdminUserDto)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createAdminUser_WithoutAuthentication_ShouldReturnForbidden() throws Exception {

    mockMvc
        .perform(
            post("/api/auth/create/admin")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdminUserDto)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createAdminUser_WithExistingUsername_ShouldHandleException() throws Exception {

    given(walletService.createAdminUser(any(CreateAdminUserDto.class)))
        .willThrow(new CustomConflictException("Username already exists"));

    mockMvc
        .perform(
            post("/api/auth/create/admin")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdminUserDto)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createAdminUser_WithInvalidDto_ShouldReturnBadRequest() throws Exception {

    CreateAdminUserDto invalidDto = new CreateAdminUserDto("", "");

    mockMvc
        .perform(
            post("/api/auth/create/admin")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createAdminUser_WithNullDto_ShouldReturnBadRequest() throws Exception {

    String invalidJson = "{}";

    mockMvc
        .perform(
            post("/api/auth/create/admin")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_WithAuthenticationException_ShouldReturnUnauthorized() throws Exception {

    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willThrow(new AuthenticationException("Authentication failed") {});

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequestDto)))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_ADMIN", "ROLE_CUSTOMER", "ROLE_USER"})
  void login_WithDifferentUserRoles_ShouldReturnCorrectRole(String role) throws Exception {

    Authentication authentication = mock(Authentication.class);

    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willReturn(authentication);
    given(authentication.getAuthorities())
        .willAnswer(invocation -> Collections.singletonList(new SimpleGrantedAuthority(role)));
    given(jwtTokenProvider.generateToken(anyString(), anyString())).willReturn("jwt-token-" + role);

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value(role));
  }
}
