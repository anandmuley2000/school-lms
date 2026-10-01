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
@RequestMapping("/api/results")
@Transactional
public class ResultController {
  private final ResultRepository results;
  private final StudentRepository students;
  private final NotificationRepository notifications;
  private final TeacherClassRepository teacherClasses;

  public ResultController(
      ResultRepository r, StudentRepository s, NotificationRepository n, TeacherClassRepository t) {
    results = r;
    students = s;
    notifications = n;
    teacherClasses = t;
  }

  @GetMapping
  public List<ResultView> list(@AuthenticationPrincipal Account user) {
    Set<Long> allowed = new HashSet<>();
    if (user.getRole() == Role.PARENT)
      students
          .findByParent_EmailOrderByClassNameAscRollNumberAsc(user.getEmail())
          .forEach(s -> allowed.add(s.getId()));
    if (user.getRole() == Role.STUDENT)
      students.findByStudentAccount_Email(user.getEmail()).forEach(s -> allowed.add(s.getId()));
    if (user.getRole() == Role.TEACHER)
      students.findAll().stream()
          .filter(
              s ->
                  teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
                      user.getEmail(), s.getClassName(), s.getDivision()))
          .forEach(s -> allowed.add(s.getId()));
    return results.findAll().stream()
        .filter(r -> user.getRole() == Role.ADMIN || allowed.contains(r.getStudent().getId()))
        .map(ResultView::from)
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  public ResultView create(
      @Valid @RequestBody ResultRequest request, @AuthenticationPrincipal Account user) {
    if (request.marks() > request.maximumMarks())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Marks cannot exceed maximum marks");
    Student s =
        students
            .findById(request.studentId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    if (user.getRole() == Role.TEACHER
        && !teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
            user.getEmail(), s.getClassName(), s.getDivision()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Student is outside your assigned classes");
    AssessmentResult result =
        results.save(
            new AssessmentResult(
                s,
                request.examName(),
                request.subject(),
                request.marks(),
                request.maximumMarks(),
                request.examDate(),
                user));
    if (s.getParent() != null)
      notifications.save(
          new Notification(
              s.getParent(),
              "Result updated",
              request.subject()
                  + " result for "
                  + s.getFullName()
                  + ": "
                  + request.marks()
                  + " / "
                  + request.maximumMarks()
                  + "."));
    return ResultView.from(result);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!results.existsById(id))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found");
    results.deleteById(id);
  }
}
