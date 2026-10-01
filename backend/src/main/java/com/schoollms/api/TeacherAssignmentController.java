package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/teachers/{teacherId}/classes")
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class TeacherAssignmentController {
  private final TeacherClassRepository assignments;
  private final AccountRepository accounts;

  public TeacherAssignmentController(TeacherClassRepository a, AccountRepository ac) {
    assignments = a;
    accounts = ac;
  }

  public record AssignmentView(Long id, String className, String division) {}

  @GetMapping
  public List<AssignmentView> list(@PathVariable Long teacherId) {
    Account t = teacher(teacherId);
    return assignments.findByTeacher_Email(t.getEmail()).stream()
        .map(a -> new AssignmentView(a.getId(), a.getClassName(), a.getDivision()))
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AssignmentView add(
      @PathVariable Long teacherId, @Valid @RequestBody TeacherAssignmentRequest r) {
    Account t = teacher(teacherId);
    if (assignments.existsByTeacher_EmailAndClassNameAndDivision(
        t.getEmail(), r.className(), r.division()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Class already assigned");
    TeacherClass a = assignments.save(new TeacherClass(t, r.className(), r.division()));
    return new AssignmentView(a.getId(), a.getClassName(), a.getDivision());
  }

  @DeleteMapping("/{assignmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Long teacherId, @PathVariable Long assignmentId) {
    teacher(teacherId);
    TeacherClass a =
        assignments
            .findById(assignmentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    if (!a.getTeacherEmail().equals(accounts.findById(teacherId).orElseThrow().getEmail()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Assignment belongs to another teacher");
    assignments.delete(a);
  }

  private Account teacher(Long id) {
    Account t =
        accounts
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
    if (t.getRole() != Role.TEACHER)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not a teacher");
    return t;
  }
}
