package com.ing.hubs.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.ing.hubs.entity.AdminUser;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminUserRepositoryTest {

  @Autowired private AdminUserRepository adminUserRepository;

  @Autowired private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {

    AdminUser testAdmin =
        AdminUser.builder().id("admin-1").username("adminUser").password("adminPass123").build();

    entityManager.persist(testAdmin);
    entityManager.flush();
  }

  @Test
  void findByUsername_WhenUsernameExists_ShouldReturnAdminUser() {

    Optional<AdminUser> found = adminUserRepository.findByUsername("adminUser");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("adminUser");
    assertThat(found.get().getPassword()).isEqualTo("adminPass123");
  }

  @ParameterizedTest
  @ValueSource(strings = {"nonExistentAdmin", "ADMINUSER"})
  void findByUsername_WhenUsernameNotExists_ShouldReturnEmpty(String username) {

    Optional<AdminUser> found = adminUserRepository.findByUsername(username);

    assertThat(found).isEmpty();
  }
}
