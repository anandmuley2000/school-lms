package com.schoollms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

class ProductionConfigurationTest {

  @Test
  void productionProfileResolvesRenderPortAndMySqlTlsUrlFromEnvironment() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");

      TestPropertyValues.of(
              "SPRING_DATASOURCE_URL=jdbc:mysql://db.example.invalid:15432/school?sslMode=VERIFY_IDENTITY&serverTimezone=UTC",
              "SPRING_DATASOURCE_USERNAME=environment-user",
              "SPRING_DATASOURCE_PASSWORD=environment-password",
              "PORT=10000",
              "JWT_SECRET=test-only-secret",
              "CORS_ALLOWED_ORIGINS=https://admin.example.invalid")
          .applyTo(context.getEnvironment());

      new ConfigDataApplicationContextInitializer().initialize(context);

      assertEquals(
          "10000",
          context.getEnvironment().getProperty("server.port"));

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
    }
  }

  @Test
  void productionProfileUsesSafeProductionSettings() {
    try (var context = new GenericApplicationContext()) {
      context.getEnvironment().setActiveProfiles("production");

      TestPropertyValues.of(
              "SPRING_DATASOURCE_URL=jdbc:mysql://db.example.invalid:15432/school?sslMode=VERIFY_IDENTITY&serverTimezone=UTC",
              "SPRING_DATASOURCE_USERNAME=environment-user",
              "SPRING_DATASOURCE_PASSWORD=environment-password",
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
    }
  }
}
