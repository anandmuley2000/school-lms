package com.schoollms.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoollms.config.SecurityConfig;
import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import com.schoollms.repository.AccountRepository;
import com.schoollms.security.JwtAuthFilter;
import com.schoollms.security.JwtService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(
    controllers = AuthController.class,
    properties = {
      "app.jwt.secret=test-only-jwt-secret-with-more-than-32-bytes",
      "app.jwt.expiration-ms=3600000",
      "app.cors.allowed-origins=https://admin.northstar.school",
      "spring.profiles.active=production"
    })
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class, ApiExceptionHandler.class})
class AuthLoginFlowTest {
  private static final DemoLogin[] DEMO_LOGINS = {
    new DemoLogin("admin@northstar.local", "Admin@123", Role.ADMIN),
    new DemoLogin("teacher@northstar.local", "Teacher@123", Role.TEACHER),
    new DemoLogin("parent@northstar.local", "Parent@123", Role.PARENT)
  };

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  @MockitoBean private AccountRepository accounts;

  private final Map<String, Account> demoAccounts = new LinkedHashMap<>();

  @BeforeEach
  void setUpAccountsWithApplicationBcryptEncoder() {
    demoAccounts.clear();
    long id = 1;
    for (DemoLogin login : DEMO_LOGINS) {
      String passwordHash = passwordEncoder.encode(login.password());
      assertThat(passwordHash).startsWith("$2");
      assertThat(passwordEncoder.matches(login.password(), passwordHash)).isTrue();

      Account account = new Account(login.email(), passwordHash, login.role(), login.role() + " Demo", null);
      ReflectionTestUtils.setField(account, "id", id++);
      demoAccounts.put(login.email(), account);
    }
    when(accounts.findByEmailIgnoreCase(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(demoAccounts.get(invocation.getArgument(0))));
  }

  @Test
  void actuatorHealthIsPublicWithoutJwtAuthentication() throws Exception {
    // This MVC slice does not install Actuator's endpoint handler, so 404 proves
    // the request passed the security chain; a protected request would be 401.
    mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
  }

  @Test
  void allDemoAccountsAuthenticateAndReceiveJwtWithTheirRole() throws Exception {
    for (DemoLogin login : DEMO_LOGINS) {
      MvcResult result =
          mvc.perform(
                  post("/api/auth/login")
                      .contentType("application/json")
                      .content(
                          objectMapper.writeValueAsBytes(
                              Map.of("email", login.email(), "password", login.password()))))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.token").isString())
              .andExpect(jsonPath("$.tokenType").value("Bearer"))
              .andExpect(jsonPath("$.user.email").value(login.email()))
              .andExpect(jsonPath("$.user.role").value(login.role().name()))
              .andReturn();

      JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
      String token = body.path("token").asText();
      assertThat(token).isNotBlank();
      assertThat(jwtService.subject(token)).isEqualTo(login.email());
      assertThat(demoAccounts).containsKey(login.email());
      assertThat(passwordEncoder.matches(login.password(), demoAccounts.get(login.email()).getPassword()))
          .isTrue();
    }
  }

  @Test
  void invalidPasswordRemainsUnauthorized() throws Exception {
    for (DemoLogin login : DEMO_LOGINS) {
      mvc.perform(
              post("/api/auth/login")
                  .contentType("application/json")
                  .content(
                      objectMapper.writeValueAsBytes(
                          Map.of("email", login.email(), "password", "wrong-password"))))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Email or password is incorrect"));
    }
  }

  @Test
  void emailLookupIsCaseInsensitive() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsBytes(
                        Map.of("email", "ADMIN@NORTHSTAR.LOCAL", "password", "Admin@123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.role").value("ADMIN"));
  }

  @Test
  void productionCorsAllowsAndroidWebViewPreflightForLogin() throws Exception {
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", "https://localhost")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://localhost"))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
        .andExpect(header().string("Access-Control-Allow-Headers", containsString("content-type")));
  }

  private record DemoLogin(String email, String password, Role role) {}
}
