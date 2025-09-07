package com.ing.hubs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomNotFoundException extends ResponseStatusException {

  public CustomNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }
}
