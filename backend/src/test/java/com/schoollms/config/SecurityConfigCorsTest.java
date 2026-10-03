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
  void productionRequiresActualHttpsOriginsAndRejectsWildcardAndNonHttpsOrigins() {
    assertThrows(
        IllegalStateException.class,
        () -> securityConfig.corsConfigurationSource("", productionEnvironment));
    assertThrows(
        IllegalStateException.class,
        () -> securityConfig.corsConfigurationSource("*", productionEnvironment));
    assertThrows(
        IllegalStateException.class,
        () -> securityConfig.corsConfigurationSource("http://admin.example.invalid", productionEnvironment));
    assertThrows(
        IllegalStateException.class,
        () -> securityConfig.corsConfigurationSource("https://YOUR-ADMIN-DOMAIN.onrender.com", productionEnvironment));
    assertThrows(
        IllegalStateException.class,
        () -> securityConfig.corsConfigurationSource("https://127.0.0.1", productionEnvironment));
    assertDoesNotThrow(
        () ->
            securityConfig.corsConfigurationSource(
                "https://school-admin.test,https://localhost",
                productionEnvironment));
  }
}
