package com.ing.hubs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomUnauthorizedException extends ResponseStatusException {

  public CustomUnauthorizedException(String reason) {
    super(HttpStatus.UNAUTHORIZED, reason);
  }
}
