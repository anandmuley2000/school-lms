package com.schoollms.config;

import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import com.schoollms.repository.AccountRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** One-time, explicitly enabled bootstrap for the first production administrator. */
@Component
@Profile("production")
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class ProductionAdminBootstrap implements ApplicationRunner {
  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final String email;
  private final String password;
  private final String name;

  public ProductionAdminBootstrap(
      AccountRepository accounts,
      PasswordEncoder passwordEncoder,
      @Value("${app.bootstrap-admin.email:}") String email,
      @Value("${app.bootstrap-admin.password:}") String password,
      @Value("${app.bootstrap-admin.name:}") String name) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.email = email;
    this.password = password;
    this.name = name;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (accounts.countByRole(Role.ADMIN) > 0) {
      return;
    }

    String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    String normalizedName = name == null ? "" : name.trim();
    if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
      throw new IllegalStateException(
          "A valid BOOTSTRAP_ADMIN_EMAIL is required when admin bootstrap is enabled");
    }
    if (password == null || password.length() < 12) {
      throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
    }
    if (normalizedName.isBlank()) {
      throw new IllegalStateException(
          "BOOTSTRAP_ADMIN_NAME is required when admin bootstrap is enabled");
    }
    if (accounts.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
      throw new IllegalStateException(
          "The bootstrap email already belongs to a non-admin account; choose a different email");
    }

    String encodedPassword = passwordEncoder.encode(password);
    accounts.save(new Account(normalizedEmail, encodedPassword, Role.ADMIN, normalizedName, null));
  }
}
