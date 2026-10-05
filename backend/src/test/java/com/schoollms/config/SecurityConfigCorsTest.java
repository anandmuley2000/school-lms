package com.schoollms.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

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

  @Test
  void developmentWildcardUsesOriginPatternsWithCredentials() {
    var developmentEnvironment = new MockEnvironment();
    developmentEnvironment.setActiveProfiles("development");
    var source = securityConfig.corsConfigurationSource("*", developmentEnvironment);
    var request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
    var cors = source.getCorsConfiguration(request);

    assertTrue(cors.getAllowedOrigins() == null || cors.getAllowedOrigins().isEmpty());
    assertEquals(java.util.List.of("*"), cors.getAllowedOriginPatterns());
    assertTrue(cors.getAllowCredentials());
    assertEquals("http://localhost:8081", cors.checkOrigin("http://localhost:8081"));
  }
  @Test
  void productionAllowsCapacitorAndroidOriginAlongsideExplicitWebOrigins() {
    var source =
        securityConfig.corsConfigurationSource(
            "https://admin.northstar.school", productionEnvironment);
    var request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
    var cors = source.getCorsConfiguration(request);

    assertTrue(cors.getAllowedOrigins().contains("https://admin.northstar.school"));
    assertTrue(cors.getAllowedOrigins().contains("https://localhost"));
    assertEquals("https://localhost", cors.checkOrigin("https://localhost"));
    assertTrue(cors.getAllowedMethods().contains("POST"));
    assertTrue(cors.checkHeaders(java.util.List.of("content-type")).contains("content-type"));
  }
}
