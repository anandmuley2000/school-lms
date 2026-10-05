package com.schoollms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import com.schoollms.repository.AccountRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AccountControllerTest {
  @Test
  void lastAdminCannotBeDemoted() {
    AccountRepository accounts = mock(AccountRepository.class);
    Account admin = mock(Account.class);
    when(accounts.findById(42L)).thenReturn(Optional.of(admin));
    when(admin.getRole()).thenReturn(Role.ADMIN);
    when(admin.getEmail()).thenReturn("admin@example.test");
    when(accounts.countByRole(Role.ADMIN)).thenReturn(1L);

    AccountController controller = new AccountController(
        accounts, mock(PasswordEncoder.class), mock(StudentRepository.class),
        mock(TeacherClassRepository.class));

    ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
        controller.update(42L, new AccountRequest("admin@example.test", "Admin", null,
            Role.TEACHER, null)));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(accounts, never()).save(admin);
  }

  @Test
  void lastAdminCannotBeDeleted() {
    AccountRepository accounts = mock(AccountRepository.class);
    Account admin = mock(Account.class);
    when(accounts.findById(42L)).thenReturn(Optional.of(admin));
    when(admin.getRole()).thenReturn(Role.ADMIN);
    when(accounts.countByRole(Role.ADMIN)).thenReturn(1L);

    AccountController controller = new AccountController(
        accounts, mock(PasswordEncoder.class), mock(StudentRepository.class),
        mock(TeacherClassRepository.class));

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> controller.delete(42L));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(accounts, never()).delete(admin);
  }
}
