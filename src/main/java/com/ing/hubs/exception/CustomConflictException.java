package com.ing.hubs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomConflictException extends ResponseStatusException {

  public CustomConflictException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }
}
