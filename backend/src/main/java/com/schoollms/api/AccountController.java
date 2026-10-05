package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class AccountController {
  private final AccountRepository accounts;
  private final PasswordEncoder encoder;
  private final StudentRepository students;
  private final TeacherClassRepository teacherClasses;

  public AccountController(
      AccountRepository accounts,
      PasswordEncoder encoder,
      StudentRepository students,
      TeacherClassRepository teacherClasses) {
    this.accounts = accounts;
    this.encoder = encoder;
    this.students = students;
    this.teacherClasses = teacherClasses;
  }

  @GetMapping
  public List<AccountView> list() {
    return accounts.findAll().stream().map(AccountView::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountView create(@Valid @RequestBody AccountRequest r) {
    if (r.role() == Role.STUDENT)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Create a parent account and link students to that family");
    if (r.password() == null || r.password().length() < 8)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters");
    if (accounts.findByEmail(r.email()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
    return AccountView.from(
        accounts.save(
            new Account(
                r.email().toLowerCase(),
                encoder.encode(r.password()),
                r.role(),
                r.name(),
                r.phone())));
  }

  @PutMapping("/{id}")
  public AccountView update(@PathVariable Long id, @Valid @RequestBody AccountRequest r) {
    if (r.role() == Role.STUDENT)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Student portal accounts are no longer supported");
    Account a =
        accounts
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    if (!a.getEmail().equalsIgnoreCase(r.email()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be changed");
    a.updateProfile(r.name(), r.phone(), r.role());
    if (r.password() != null && !r.password().isBlank()) {
      if (r.password().length() < 8)
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters");
      a.changePassword(encoder.encode(r.password()));
    }
    return AccountView.from(accounts.save(a));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    Account a =
        accounts
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    if (a.getRole() == Role.ADMIN && accounts.countByRole(Role.ADMIN) <= 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "At least one administrator must remain");
    if (students.existsByParent_Id(id)
        || students.existsByStudentAccount_Id(id)
        || teacherClasses.existsByTeacher_Id(id))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Remove this account's student or class assignments before deleting it");
    accounts.delete(a);
  }
}
