package com.ing.hubs.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLockingException(
      ObjectOptimisticLockingFailureException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message("Operation failed due to concurrent modification. Please try again.")
            .errorCode("CONCURRENT_MODIFICATION")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Optimistic locking failure: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message("Database operation failed. Please try again later.")
            .errorCode("DATABASE_ERROR")
            .timestamp(LocalDateTime.now())
            .build();

    log.error("Database error: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(CustomNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCustomNotFoundException(CustomNotFoundException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message(ex.getReason())
            .errorCode("NOT_FOUND")
            .timestamp(LocalDateTime.now())
            .build();

    log.info("Resource not found: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(CustomUnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleCustomUnauthorizedException(
      CustomUnauthorizedException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message(ex.getReason())
            .errorCode("UNAUTHORIZED")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Unauthorized access attempt: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(CustomConflictException.class)
  public ResponseEntity<ErrorResponse> handleCustomConflictException(CustomConflictException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message(ex.getReason())
            .errorCode("CONFLICT")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Conflict detected: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(CustomBadRequestException.class)
  public ResponseEntity<ErrorResponse> handleCustomBadRequestException(
      CustomBadRequestException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message(ex.getReason())
            .errorCode("BAD_REQUEST")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Bad request: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message("Invalid username or password")
            .errorCode("AUTHENTICATION_FAILED")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Authentication failed: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
      AuthorizationDeniedException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message("Access denied. Insufficient permissions.")
            .errorCode("ACCESS_DENIED")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Authorization denied: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      @Nullable HttpHeaders headers,
      @Nullable HttpStatusCode status,
      @Nullable WebRequest request) {

    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fieldError ->
                        fieldError.getDefaultMessage() != null
                            ? fieldError.getDefaultMessage()
                            : "Invalid value"));

    ErrorResponse error =
        ErrorResponse.builder()
            .message("Validation failed")
            .errorCode("VALIDATION_ERROR")
            .details(errors)
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Validation failed - errors: {}", errors, ex);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      @Nullable HttpHeaders headers,
      @Nullable HttpStatusCode status,
      @Nullable WebRequest request) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message("Invalid JSON format")
            .errorCode("INVALID_JSON")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("JSON parse error: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message("Invalid request parameter: " + ex.getMessage())
            .errorCode("INVALID_PARAMETER")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Invalid parameter: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {

    ErrorResponse error =
        ErrorResponse.builder()
            .message("An unexpected error occurred. Please try again later.")
            .errorCode("INTERNAL_SERVER_ERROR")
            .timestamp(LocalDateTime.now())
            .build();

    log.warn("Unexpected error: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
