package com.schoollms.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<?> authentication() {
    return ResponseEntity.status(401)
        .body(
            Map.of(
                "status",
                401,
                "message",
                "Email or password is incorrect",
                "timestamp",
                Instant.now().toString()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> forbidden() {
    return ResponseEntity.status(403)
        .body(
            Map.of(
                "status",
                403,
                "message",
                "You do not have access to this resource",
                "timestamp",
                Instant.now().toString()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            Map.of(
                "status",
                e.getStatusCode().value(),
                "message",
                e.getReason() == null ? "Request failed" : e.getReason(),
                "timestamp",
                Instant.now().toString()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("Invalid request");
    return ResponseEntity.badRequest()
        .body(Map.of("status", 400, "message", message, "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "status",
                409,
                "message",
                "The change conflicts with existing data",
                "timestamp",
                Instant.now().toString()));
  }

  @ExceptionHandler({HttpMessageNotReadableException.class})
  ResponseEntity<?> malformed() {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "status",
                400,
                "message",
                "Request body is invalid",
                "timestamp",
                Instant.now().toString()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<?> missing() {
    return ResponseEntity.status(404)
        .body(
            Map.of(
                "status",
                404,
                "message",
                "Resource not found",
                "timestamp",
                Instant.now().toString()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    return ResponseEntity.internalServerError()
        .body(
            Map.of(
                "status",
                500,
                "message",
                "An unexpected server error occurred",
                "timestamp",
                Instant.now().toString()));
  }
}
