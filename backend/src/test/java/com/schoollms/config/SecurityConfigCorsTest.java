package com.schoollms.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityConfigCorsTest {
  private final SecurityConfig securityConfig = new SecurityConfig();
  private final MockEnvironment productionEnvironment = new MockEnvironment();

  SecurityConfigCorsTest() {
    productionEnvironment.setActiveProfiles("production");
  }

  @Test
  void productionAllowsNoOriginsButRejectsWildcardAndNonHttpsOrigins() {
    assertDoesNotThrow(() -> securityConfig.corsConfigurationSource("", productionEnvironment));
    assertThrows(
        IllegalStateException.class,
        () -> securityConfig.corsConfigurationSource("*", productionEnvironment));
    assertThrows(
        IllegalStateException.class,
        () -> securityConfig.corsConfigurationSource("http://admin.example.invalid", productionEnvironment));
    assertDoesNotThrow(
        () ->
            securityConfig.corsConfigurationSource(
                "https://admin.example.invalid,https://staff.example.invalid",
                productionEnvironment));
  }
}
