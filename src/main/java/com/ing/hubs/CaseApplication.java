package com.ing.hubs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class CaseApplication {

  public static void main(String[] args) {
    SpringApplication.run(CaseApplication.class, args);
  }
}
