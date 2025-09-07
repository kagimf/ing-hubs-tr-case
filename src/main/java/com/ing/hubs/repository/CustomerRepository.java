package com.ing.hubs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ing.hubs.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, String> {

  Optional<Customer> findByTckn(String tckn);

  @Query("SELECT c.id FROM Customer c WHERE c.tckn = :tckn")
  Optional<String> findCustomerIdByTckn(String tckn);
}
