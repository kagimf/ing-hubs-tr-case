package com.ing.hubs.exception;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

  private String message;
  private String errorCode;
  private LocalDateTime timestamp;

  @JsonInclude(Include.NON_NULL)
  private Map<String, String> details;
}
