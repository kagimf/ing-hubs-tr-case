package com.ing.hubs.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import com.ing.hubs.entity.AdminUser;
import com.ing.hubs.entity.Customer;
import com.ing.hubs.exception.CustomNotFoundException;
import com.ing.hubs.repository.AdminUserRepository;
import com.ing.hubs.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock private CustomerRepository customerRepository;

  @Mock private AdminUserRepository adminUserRepository;

  @InjectMocks private CustomUserDetailsService customUserDetailsService;

  @Test
  void loadUserByUsername_WhenAdminUserExists_ShouldReturnAdminUserDetails() {

    String username = "adminUser";
    String password = "adminPass123";
    AdminUser adminUser =
        AdminUser.builder().id("admin1").username(username).password(password).build();

    given(adminUserRepository.findByUsername(username)).willReturn(Optional.of(adminUser));

    UserDetails result = customUserDetailsService.loadUserByUsername(username);

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo(username);
    assertThat(result.getPassword()).isEqualTo(password);
    assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
  }

  @Test
  void loadUserByUsername_WhenCustomerExists_ShouldReturnCustomerUserDetails() {

    String tckn = "12345678901";
    String password = "customerPass123";
    Customer customer =
        Customer.builder()
            .id("cust1")
            .tckn(tckn)
            .password(password)
            .name("John")
            .surname("Doe")
            .build();

    given(adminUserRepository.findByUsername(tckn)).willReturn(Optional.empty());
    given(customerRepository.findByTckn(tckn)).willReturn(Optional.of(customer));

    UserDetails result = customUserDetailsService.loadUserByUsername(tckn);

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo(tckn);
    assertThat(result.getPassword()).isEqualTo(password);
    assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
  }

  @Test
  void loadUserByUsername_WhenNeitherAdminNorCustomerExists_ShouldThrowCustomNotFoundException() {

    String username = "nonExistentUser";

    given(adminUserRepository.findByUsername(username)).willReturn(Optional.empty());
    given(customerRepository.findByTckn(username)).willReturn(Optional.empty());

    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
        .isInstanceOf(CustomNotFoundException.class)
        .hasMessageContaining("User not found with username: " + username);
  }

  @Test
  void loadUserByUsername_WhenAdminUserExistsWithValidEmptyPassword_ShouldReturnUserDetails() {

    String username = "adminUser";
    String emptyPassword = " ";

    AdminUser adminUser =
        AdminUser.builder().id("admin1").username(username).password(emptyPassword).build();

    given(adminUserRepository.findByUsername(username)).willReturn(Optional.of(adminUser));

    UserDetails result = customUserDetailsService.loadUserByUsername(username);

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo(username);
    assertThat(result.getPassword()).isEqualTo(emptyPassword);
    assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
  }

  @Test
  void loadUserByUsername_WhenCustomerExistsWithValidEmptyPassword_ShouldReturnUserDetails() {

    String tckn = "12345678901";
    String emptyPassword = " ";

    Customer customer =
        Customer.builder()
            .id("cust1")
            .tckn(tckn)
            .password(emptyPassword)
            .name("John")
            .surname("Doe")
            .build();

    given(adminUserRepository.findByUsername(tckn)).willReturn(Optional.empty());
    given(customerRepository.findByTckn(tckn)).willReturn(Optional.of(customer));

    UserDetails result = customUserDetailsService.loadUserByUsername(tckn);

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo(tckn);
    assertThat(result.getPassword()).isEqualTo(emptyPassword);
    assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
  }
}
