package com.schoollms.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import com.schoollms.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoAccountInitializerTest {
  private AccountRepository accounts;
  private PasswordEncoder passwordEncoder;
  private DemoAccountInitializer initializer;

  @BeforeEach
  void setUp() {
    accounts = mock(AccountRepository.class);
    passwordEncoder = new SecurityConfig().passwordEncoder();
    initializer = new DemoAccountInitializer(accounts, passwordEncoder);
    when(accounts.findByEmail(anyString())).thenReturn(Optional.empty());
  }

  @Test
  void insertsOnlyMissingAccountsWithBcryptHashesAndActiveStatus() throws Exception {
    initializer.run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
    verify(accounts, times(3)).save(saved.capture());
    assertEquals(3, saved.getAllValues().size());

    assertDemoAccount(
        saved.getAllValues().get(0),
        "admin@northstar.local",
        "Admin@123",
        Role.ADMIN,
        "System Administrator");
    assertDemoAccount(
        saved.getAllValues().get(1),
        "teacher@northstar.local",
        "Teacher@123",
        Role.TEACHER,
        "Demo Teacher");
    assertDemoAccount(
        saved.getAllValues().get(2),
        "student@northstar.local",
        "Student@123",
        Role.STUDENT,
        "Demo Student");
  }

  @Test
  void preservesAccountsThatAlreadyExist() throws Exception {
    Account existing =
        new Account(
            "admin@northstar.local",
            passwordEncoder.encode("existing-password"),
            Role.ADMIN,
            "Existing Administrator",
            null);
    when(accounts.findByEmail("admin@northstar.local")).thenReturn(Optional.of(existing));

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(accounts, times(3)).findByEmail(anyString());
    verify(accounts, times(2)).save(any(Account.class));
    assertEquals("Existing Administrator", existing.getName());
    assertTrue(passwordEncoder.matches("existing-password", existing.getPassword()));
  }

  @Test
  void generatedPasswordHashAuthenticatesThroughSpringSecurityDaoProvider() throws Exception {
    initializer.run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
    verify(accounts, times(3)).save(saved.capture());

    DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(new SecurityConfig().userDetailsService(accounts));
    provider.setPasswordEncoder(passwordEncoder);
    AuthenticationManager manager = new ProviderManager(provider);

    List<String> passwords = List.of("Admin@123", "Teacher@123", "Student@123");
    for (int i = 0; i < saved.getAllValues().size(); i++) {
      Account account = saved.getAllValues().get(i);
      when(accounts.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
      assertTrue(
          manager
              .authenticate(
                  new UsernamePasswordAuthenticationToken(account.getEmail(), passwords.get(i)))
              .isAuthenticated());
    }
  }

  private void assertDemoAccount(
      Account account, String email, String rawPassword, Role role, String name) {
    assertEquals(email, account.getEmail());
    assertEquals(role, account.getRole());
    assertEquals(name, account.getName());
    assertTrue(account.isEnabled());
    assertNotEquals(rawPassword, account.getPassword());
    assertTrue(passwordEncoder.matches(rawPassword, account.getPassword()));
    assertTrue(account.getPassword().startsWith("$2"));
  }
}
