package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/fees")
@Transactional
public class FeeController {
  private final FeeRepository fees;
  private final StudentRepository students;

  public FeeController(FeeRepository fees, StudentRepository students) {
    this.fees = fees;
    this.students = students;
  }

  @GetMapping
  public List<FeeView> list(@AuthenticationPrincipal Account user) {
    Set<Long> allowed = new HashSet<>();
    if (user.getRole() == Role.PARENT)
      students
          .findByParent_EmailOrderByClassNameAscRollNumberAsc(user.getEmail())
          .forEach(s -> allowed.add(s.getId()));
    if (user.getRole() == Role.STUDENT)
      students.findByStudentAccount_Email(user.getEmail()).forEach(s -> allowed.add(s.getId()));
    return fees.findAll().stream()
        .filter(f -> user.getRole() == Role.ADMIN || allowed.contains(f.getStudent().getId()))
        .map(FeeView::from)
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public FeeView create(@Valid @RequestBody FeeRequest r) {
    Student s =
        students
            .findById(r.studentId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    FeeRecord f = new FeeRecord(s, r.description(), r.amount(), r.dueDate());
    if (r.status() != null) f.setStatus(r.status());
    return FeeView.from(fees.save(f));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public FeeView update(@PathVariable Long id, @RequestParam FeeStatus status) {
    FeeRecord f =
        fees.findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
    f.setStatus(status);
    return FeeView.from(fees.save(f));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!fees.existsById(id))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found");
    fees.deleteById(id);
  }
}
