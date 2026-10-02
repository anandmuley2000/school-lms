package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/students")
@Transactional
public class StudentController {
  private final StudentRepository students;
  private final AccountRepository accounts;
  private final TeacherClassRepository teacherClasses;
  private final AttendanceRepository attendance;
  private final ResultRepository results;
  private final FeeRepository fees;
  private final PasswordEncoder passwordEncoder;

  public StudentController(
      StudentRepository students,
      AccountRepository accounts,
      TeacherClassRepository teacherClasses,
      AttendanceRepository attendance,
      ResultRepository results,
      FeeRepository fees,
      PasswordEncoder passwordEncoder) {
    this.students = students;
    this.accounts = accounts;
    this.teacherClasses = teacherClasses;
    this.attendance = attendance;
    this.results = results;
    this.fees = fees;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping
  public List<StudentView> list(@AuthenticationPrincipal Account user) {
    List<Student> result =
        switch (user.getRole()) {
          case ADMIN -> students.findAll();
          case PARENT ->
              students.findByParent_EmailOrderByClassNameAscRollNumberAsc(user.getEmail());
          case STUDENT -> students.findByStudentAccount_Email(user.getEmail());
          case TEACHER ->
              students.findAll().stream()
                  .filter(
                      s ->
                          teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
                              user.getEmail(), s.getClassName(), s.getDivision()))
                  .toList();
        };
    return result.stream().map(StudentView::from).toList();
  }

  @GetMapping("/{id}")
  public StudentView get(@PathVariable Long id, @AuthenticationPrincipal Account user) {
    Student s =
        students
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    if (!canAccess(s, user))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only access students in your account or assigned classes");
    return StudentView.from(s);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public StudentView create(@Valid @RequestBody StudentRequest r) {
    if (students.findByStudentCode(r.studentCode()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Student ID already exists");
    if (r.studentLoginEmail() == null || r.studentLoginEmail().isBlank())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Student login email is required when creating a student");
    if (r.studentInitialPassword() == null || r.studentInitialPassword().length() < 8)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Student initial password must contain at least 8 characters");
    String studentEmail = r.studentLoginEmail().trim().toLowerCase();
    if (accounts.findByEmail(studentEmail).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Student login email is already in use");
    Account studentAccount =
        accounts.save(
            new Account(
                studentEmail,
                passwordEncoder.encode(r.studentInitialPassword()),
                Role.STUDENT,
                (r.firstName() + " " + r.lastName()).trim(),
                r.phone()));
    Student s =
        new Student(
            r.studentCode(),
            r.firstName(),
            r.lastName(),
            r.className(),
            r.division(),
            r.rollNumber(),
            parent(r.parentEmail()));
    s.setStudentAccount(studentAccount);
    s.update(
        r.firstName(),
        r.lastName(),
        r.className(),
        r.division(),
        r.rollNumber(),
        r.phone(),
        r.address(),
        r.status() == null ? "ACTIVE" : r.status());
    return StudentView.from(students.save(s));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public StudentView update(@PathVariable Long id, @Valid @RequestBody StudentRequest r) {
    Student s =
        students
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    if (!s.getStudentCode().equals(r.studentCode())
        && students.findByStudentCode(r.studentCode()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Student ID already exists");
    s.update(
        r.firstName(),
        r.lastName(),
        r.className(),
        r.division(),
        r.rollNumber(),
        r.phone(),
        r.address(),
        r.status() == null ? s.getStatus() : r.status());
    s.setParent(parent(r.parentEmail()));
    return StudentView.from(students.save(s));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!students.existsById(id))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
    attendance.deleteByStudent_Id(id);
    results.deleteByStudent_Id(id);
    fees.deleteByStudent_Id(id);
    students.deleteById(id);
  }

  private Account parent(String email) {
    if (email == null || email.isBlank()) return null;
    return accounts
        .findByEmail(email)
        .filter(a -> a.getRole() == Role.PARENT)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent account not found"));
  }

  private boolean canAccess(Student s, Account user) {
    return switch (user.getRole()) {
      case ADMIN -> true;
      case PARENT -> s.getParent() != null && s.getParent().getId().equals(user.getId());
      case STUDENT ->
          s.getStudentAccount() != null && s.getStudentAccount().getId().equals(user.getId());
      case TEACHER ->
          teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
              user.getEmail(), s.getClassName(), s.getDivision());
    };
  }
}
