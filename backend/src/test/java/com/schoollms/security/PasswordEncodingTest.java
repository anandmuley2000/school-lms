package com.schoollms.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncodingTest {
  @Test
  void demoPasswordIsHashedAndCanBeChecked() {
    var encoder = new BCryptPasswordEncoder();
    String raw = "Admin@123";
    String encoded = encoder.encode(raw);
    assertNotEquals(raw, encoded);
    assertTrue(encoder.matches(raw, encoded));
  }
}
