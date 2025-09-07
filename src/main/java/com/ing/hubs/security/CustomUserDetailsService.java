package com.ing.hubs.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.ing.hubs.entity.AdminUser;
import com.ing.hubs.entity.Customer;
import com.ing.hubs.exception.CustomNotFoundException;
import com.ing.hubs.repository.AdminUserRepository;
import com.ing.hubs.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final CustomerRepository customerRepository;
  private final AdminUserRepository adminUserRepository;

  @Override
  public UserDetails loadUserByUsername(String username) {

    Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);

    if (adminUser.isPresent()) {

      return createUserDetails(
          adminUser.get().getUsername(), adminUser.get().getPassword(), "ADMIN");
    }

    Optional<Customer> customer = customerRepository.findByTckn(username);

    if (customer.isPresent()) {

      return createUserDetails(customer.get().getTckn(), customer.get().getPassword(), "CUSTOMER");
    }

    throw new CustomNotFoundException("User not found with username: " + username);
  }

  private UserDetails createUserDetails(String username, String password, String role) {

    return User.builder().username(username).password(password).roles(role).build();
  }
}
