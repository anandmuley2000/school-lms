package com.schoollms.api;

import com.schoollms.domain.Account;
import com.schoollms.repository.AccountRepository;
import com.schoollms.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Locale;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthenticationManager auth;
  private final JwtService jwt;
  private final AccountRepository accounts;

  public AuthController(AuthenticationManager auth, JwtService jwt, AccountRepository accounts) {
    this.auth = auth;
    this.jwt = jwt;
    this.accounts = accounts;
  }

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  @PostMapping("/login")
  public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    auth.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
    Account a = accounts.findByEmailIgnoreCase(email).orElseThrow();
    return Map.of(
        "token",
        jwt.issue(a),
        "tokenType",
        "Bearer",
        "user",
        Map.of(
            "id",
            a.getId(),
            "email",
            a.getEmail(),
            "name",
            a.getName(),
            "role",
            a.getRole().name()));
  }

  @GetMapping("/me")
  public Map<String, Object> me(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Account a) {
    return Map.of(
        "id", a.getId(), "email", a.getEmail(), "name", a.getName(), "role", a.getRole().name());
  }
}
