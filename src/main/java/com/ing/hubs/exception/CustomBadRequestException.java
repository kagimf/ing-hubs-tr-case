package com.ing.hubs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomBadRequestException extends ResponseStatusException {

  public CustomBadRequestException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}
