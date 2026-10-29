package com.schoollms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

class ProductionConfigurationTest {

  @Test
  void productionProfileResolvesRailwayPortAndMySqlTlsUrlFromEnvironment() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");

      TestPropertyValues.of(
              "DB_HOST=db.example.invalid",
              "DB_PORT=15432",
              "DB_NAME=school",
              "DB_SSL_MODE=VERIFY_IDENTITY",
              "DB_USERNAME=environment-user",
              "DB_PASSWORD=environment-password",
              "PORT=10000",
              "JWT_SECRET=test-only-secret",
              "CORS_ALLOWED_ORIGINS=https://admin.example.invalid")
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

      assertEquals(
          "10000",
          context.getEnvironment().getProperty("server.port"));

      assertEquals(
          "0.0.0.0",
          context.getEnvironment().getProperty("server.address"));

      assertEquals(
          "jdbc:mysql://db.example.invalid:15432/school?sslMode=VERIFY_IDENTITY&serverTimezone=UTC",
          context.getEnvironment().getProperty("spring.datasource.url"));

      assertEquals(
          "environment-user",
          context.getEnvironment().getProperty("spring.datasource.username"));

      assertEquals(
          "false",
          context.getEnvironment().getProperty("app.demo.data-enabled"));

      assertEquals(
          "environment-password",
          context.getEnvironment().getProperty("spring.datasource.password"));
      assertEquals(
          "health",
          context.getEnvironment().getProperty("management.endpoints.web.exposure.include"));
      assertEquals("true", context.getEnvironment().getProperty("spring.flyway.enabled"));
      assertEquals(
          "classpath:db/migration",
          context.getEnvironment().getProperty("spring.flyway.locations"));
      assertEquals(
          "false",
          context.getEnvironment().getProperty("spring.flyway.baseline-on-migrate"));
    }
  }

  @Test
  void serverUsesRailwayCompatiblePortAndBindAddressWhenPortIsNotInjected() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");

      TestPropertyValues.of(
              "DB_HOST=db.internal",
              "DB_NAME=school",
              "DB_USERNAME=environment-user",
              "DB_PASSWORD=environment-password",
              "DB_SSL_MODE=REQUIRED",
              "JWT_SECRET=test-only-secret-with-at-least-32-bytes",
              "CORS_ALLOWED_ORIGINS=https://admin.school.example")
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

      assertEquals("8080", context.getEnvironment().getProperty("server.port"));
      assertEquals("0.0.0.0", context.getEnvironment().getProperty("server.address"));
      assertEquals(
          "jdbc:mysql://db.internal:3306/school?sslMode=REQUIRED&serverTimezone=UTC",
          context.getEnvironment().getProperty("spring.datasource.url"));
      assertEquals("false", context.getEnvironment().getProperty("app.demo.data-enabled"));
    }
  }

  @Test
  void productionProfileUsesSafeProductionSettings() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");

      TestPropertyValues.of(
              "DB_HOST=db.example.invalid",
              "DB_PORT=15432",
              "DB_NAME=school",
              "DB_SSL_MODE=VERIFY_IDENTITY",
              "DB_USERNAME=environment-user",
              "DB_PASSWORD=environment-password",
              "PORT=10000",
              "JWT_SECRET=test-only-secret-with-at-least-32-bytes",
              "CORS_ALLOWED_ORIGINS=https://admin.example.invalid",
              "DEMO_DATA_ENABLED=true")
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

      assertEquals(
          "0.0.0.0",
          context.getEnvironment().getProperty("server.address"));

      assertEquals(
          "false",
          context.getEnvironment().getProperty("spring.jpa.show-sql"));

      assertEquals(
          "https://admin.example.invalid",
          context.getEnvironment().getProperty("app.cors.allowed-origins"));

      assertEquals(
          "false",
          context.getEnvironment().getProperty("app.demo.reset-enabled"));

      assertEquals(
          "true",
          context.getEnvironment().getProperty("app.demo.data-enabled"));

      assertEquals(
          "validate",
          context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
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

      assertEquals(
          "*",
          context.getEnvironment().getProperty("app.cors.allowed-origins"));

      assertEquals(
          "false",
          context.getEnvironment().getProperty("app.demo.reset-enabled"));
      assertEquals("false", context.getEnvironment().getProperty("spring.flyway.enabled"));
    }
  }
}
