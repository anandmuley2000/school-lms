package com.schoollms.config;

import com.schoollms.repository.AccountRepository;
import com.schoollms.security.JwtAuthFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private static final String CAPACITOR_ANDROID_ORIGIN = "https://localhost";

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(AccountRepository accounts) {
    return email ->
        accounts
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
    return http.csrf(c -> c.disable())
        .cors(c -> {})
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/**",
                        "/actuator/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(401, "Authentication required"))
                    .accessDeniedHandler((req, res, ex) -> res.sendError(403, "Access denied")))
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String configuredOrigins, Environment environment) {
    CorsConfiguration c = new CorsConfiguration();
    boolean production = environment.acceptsProfiles(Profiles.of("production"));
    List<String> origins =
        java.util.Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
    if (production) {
      if (origins.isEmpty()) {
        throw new IllegalStateException(
            "CORS_ALLOWED_ORIGINS must contain at least one explicit HTTPS origin in production");
      }
      for (String origin : origins) {
        try {
          java.net.URI parsed = java.net.URI.create(origin);
          String host = parsed.getHost() == null ? "" : parsed.getHost().toLowerCase(java.util.Locale.ROOT);
          boolean ipLiteral = host.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}") || host.startsWith("[");
          if (!"https".equalsIgnoreCase(parsed.getScheme())
              || parsed.getHost() == null
              || ipLiteral
              || host.endsWith(".local")
              || host.endsWith(".localhost")
              || parsed.getRawUserInfo() != null
              || (parsed.getRawPath() != null
                  && !parsed.getRawPath().isEmpty()
                  && !"/".equals(parsed.getRawPath()))
              || parsed.getRawQuery() != null
              || parsed.getRawFragment() != null
              || origin.toUpperCase(java.util.Locale.ROOT).contains("YOUR-")
              || host.equals("example.com")
              || host.equals("example.invalid")
              || host.endsWith(".example.invalid")) {
            throw new IllegalArgumentException("Invalid production origin");
          }
        } catch (IllegalArgumentException ex) {
          throw new IllegalStateException(
              "CORS_ALLOWED_ORIGINS must contain only actual HTTPS origins without paths or wildcards in production",
              ex);
        }
      }
    }
    List<String> allowedOrigins = new java.util.ArrayList<>(origins);
    if (production
        && allowedOrigins.stream()
            .noneMatch(origin -> CAPACITOR_ANDROID_ORIGIN.equalsIgnoreCase(origin))) {
      allowedOrigins.add(CAPACITOR_ANDROID_ORIGIN);
    }
    c.setAllowedOrigins(allowedOrigins);
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    c.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }
}
