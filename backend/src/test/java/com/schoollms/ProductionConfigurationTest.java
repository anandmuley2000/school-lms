package com.schoollms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

class ProductionConfigurationTest {
  @Test
  void productionProfileResolvesRenderPortAndAivenTlsUrlFromEnvironment() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");
      TestPropertyValues.of(
            "DB_HOST=db.example.invalid",
            "DB_PORT=15432",
            "DB_NAME=school",
            "DB_USERNAME=environment-user",
            "DB_PASSWORD=environment-password",
            "JWT_SECRET=test-only-secret",
            "CORS_ALLOWED_ORIGINS=https://admin.example.invalid")
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

      assertEquals("10000", context.getEnvironment().getProperty("server.port"));
      assertEquals(
          "jdbc:mysql://db.example.invalid:15432/school?sslMode=REQUIRED&serverTimezone=UTC",
          context.getEnvironment().getProperty("spring.datasource.url"));
      assertEquals(
          "environment-user", context.getEnvironment().getProperty("spring.datasource.username"));
      assertEquals(
          "environment-password", context.getEnvironment().getProperty("spring.datasource.password"));
      assertEquals("false", context.getEnvironment().getProperty("app.demo.reset-enabled"));
    }
  }
}
