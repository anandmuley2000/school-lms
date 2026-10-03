package com.schoollms.config;

import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import com.schoollms.repository.AccountRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds only the named demo accounts when explicitly enabled. Existing accounts are preserved. */
@Component
@ConditionalOnProperty(prefix = "app.demo", name = "data-enabled", havingValue = "true")
public class DemoAccountInitializer implements ApplicationRunner {
  private record DemoAccount(String email, String password, Role role, String name) {}

  private static final List<DemoAccount> DEMO_ACCOUNTS =
      List.of(
          new DemoAccount("admin@northstar.local", "Admin@123", Role.ADMIN, "System Administrator"),
          new DemoAccount("teacher@northstar.local", "Teacher@123", Role.TEACHER, "Demo Teacher"),
          new DemoAccount("student@northstar.local", "Student@123", Role.STUDENT, "Demo Student"));

  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;

  public DemoAccountInitializer(AccountRepository accounts, PasswordEncoder passwordEncoder) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    for (DemoAccount demo : DEMO_ACCOUNTS) {
      if (accounts.findByEmail(demo.email()).isEmpty()) {
        accounts.save(
            new Account(
                demo.email(),
                passwordEncoder.encode(demo.password()),
                demo.role(),
                demo.name(),
                null));
      }
    }
  }
}
