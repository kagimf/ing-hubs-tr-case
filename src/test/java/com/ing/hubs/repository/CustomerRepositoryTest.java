package com.ing.hubs.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.ing.hubs.entity.Customer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

  @Autowired private CustomerRepository customerRepository;

  @Autowired private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {

    Customer testCustomer =
        Customer.builder()
            .id("cust-123")
            .tckn("12345678901")
            .name("John")
            .surname("Doe")
            .password("customerPass123")
            .wallets(new ArrayList<>())
            .build();

    entityManager.persist(testCustomer);
    entityManager.flush();
  }

  @Test
  void findByTckn_WhenTcknExists_ShouldReturnCustomer() {

    Optional<Customer> found = customerRepository.findByTckn("12345678901");

    assertThat(found).isPresent();
    assertThat(found.get().getTckn()).isEqualTo("12345678901");
    assertThat(found.get().getName()).isEqualTo("John");
  }

  @Test
  void findByTckn_WhenTcknNotExists_ShouldReturnEmpty() {

    String nonExistentTckn = "99999999999";

    Optional<Customer> found = customerRepository.findByTckn(nonExistentTckn);

    assertThat(found).isEmpty();
  }

  @Test
  void findCustomerIdByTckn_WhenTcknExists_ShouldReturnCustomerId() {

    Optional<String> customerId = customerRepository.findCustomerIdByTckn("12345678901");

    assertThat(customerId).isPresent();
    assertThat(customerId.get()).isEqualTo("cust-123");
  }

  @Test
  void findCustomerIdByTckn_WhenTcknNotExists_ShouldReturnEmpty() {

    String nonExistentTckn = "99999999999";

    Optional<String> customerId = customerRepository.findCustomerIdByTckn(nonExistentTckn);

    assertThat(customerId).isEmpty();
  }

  @Test
  void findByTckn_WithInvalidTcknLength_ShouldReturnEmpty() {

    String invalidTckn = "12345";

    Optional<Customer> found = customerRepository.findByTckn(invalidTckn);

    assertThat(found).isEmpty();
  }
}
