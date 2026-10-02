package com.schoollms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

class ProductionConfigurationTest {
  @Test
<<<<<<< Updated upstream
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
=======
  void productionProfileUsesRailwayEnvironmentAndDynamicPort() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");
      TestPropertyValues.of(
              "PORT=43127",
              "SPRING_DATASOURCE_URL=jdbc:mysql://mysql.internal:3306/school?serverTimezone=UTC",
              "SPRING_DATASOURCE_USERNAME=railway-user",
              "SPRING_DATASOURCE_PASSWORD=test-only-password",
              "JWT_SECRET=test-only-secret-with-at-least-32-bytes",
              "CORS_ALLOWED_ORIGINS=https://admin.example.invalid")
>>>>>>> Stashed changes
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

<<<<<<< Updated upstream
      assertEquals("10000", context.getEnvironment().getProperty("server.port"));
      assertEquals(
          "jdbc:mysql://db.example.invalid:15432/school?sslMode=REQUIRED&serverTimezone=UTC",
          context.getEnvironment().getProperty("spring.datasource.url"));
      assertEquals(
          "environment-user", context.getEnvironment().getProperty("spring.datasource.username"));
      assertEquals(
          "environment-password", context.getEnvironment().getProperty("spring.datasource.password"));
=======
      assertEquals("43127", context.getEnvironment().getProperty("server.port"));
      assertEquals(
          "0.0.0.0", context.getEnvironment().getProperty("server.address"));
      assertEquals(
          "jdbc:mysql://mysql.internal:3306/school?serverTimezone=UTC",
          context.getEnvironment().getProperty("spring.datasource.url"));
      assertEquals(
          "railway-user", context.getEnvironment().getProperty("spring.datasource.username"));
      assertEquals("update", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
      assertEquals("false", context.getEnvironment().getProperty("spring.jpa.show-sql"));
      assertEquals(
          "https://admin.example.invalid",
          context.getEnvironment().getProperty("app.cors.allowed-origins"));
      assertEquals("false", context.getEnvironment().getProperty("app.demo.reset-enabled"));
    }
  }

  @Test
  void commonConfigurationUsesSafePortFallbackWhenRailwayPortIsAbsent() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");
      TestPropertyValues.of(
              "SPRING_DATASOURCE_URL=jdbc:mysql://mysql.internal:3306/school",
              "SPRING_DATASOURCE_USERNAME=railway-user",
              "SPRING_DATASOURCE_PASSWORD=test-only-password",
              "JWT_SECRET=test-only-secret-with-at-least-32-bytes",
              "CORS_ALLOWED_ORIGINS=https://admin.example.invalid")
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

      assertEquals("8080", context.getEnvironment().getProperty("server.port"));
    }
  }

  @Test
  void developmentProfileUsesAppScopedConfigProperties() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("development");
      TestPropertyValues.of(
              "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/school",
              "SPRING_DATASOURCE_USERNAME=dev-user",
              "SPRING_DATASOURCE_PASSWORD=dev-pass",
              "JWT_SECRET=test-only-secret-with-at-least-32-bytes",
              "CORS_ALLOWED_ORIGINS=*",
              "APP_DEMO_RESET_ENABLED=false")
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

      assertEquals("*", context.getEnvironment().getProperty("app.cors.allowed-origins"));
>>>>>>> Stashed changes
      assertEquals("false", context.getEnvironment().getProperty("app.demo.reset-enabled"));
    }
  }
}
