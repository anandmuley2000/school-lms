package com.schoollms.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

class JwtServiceTest {
  @Test
  void issuesAndValidatesSignedTokensForTheExpectedAccount() {
    JwtService service = new JwtService("a-test-secret-with-at-least-256-bits-000000", 60_000);
    var account =
        User.withUsername("parent@schooldemo.com").password("hash").roles("PARENT").build();
    String token = service.issue(account);
    assertEquals(account.getUsername(), service.subject(token));
    assertTrue(service.valid(token, account));
    assertFalse(
        service.valid(
            token,
            User.withUsername("other@schooldemo.com").password("hash").roles("PARENT").build()));
  }

  @Test
  void rejectsJwtSecretsThatAreTooShort() {
    assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 60_000));
  }
}
