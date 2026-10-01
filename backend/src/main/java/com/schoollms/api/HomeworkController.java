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
@RequestMapping("/api/homework")
@Transactional
public class HomeworkController {
  private final HomeworkRepository homework;
  private final StudentRepository students;
  private final NotificationRepository notifications;
  private final TeacherClassRepository teacherClasses;

  public HomeworkController(
      HomeworkRepository homework,
      StudentRepository students,
      NotificationRepository notifications,
      TeacherClassRepository teacherClasses) {
    this.homework = homework;
    this.students = students;
    this.notifications = notifications;
    this.teacherClasses = teacherClasses;
  }

  @GetMapping
  public List<HomeworkView> list(@AuthenticationPrincipal Account user) {
    Set<String> classes = new HashSet<>();
    if (user.getRole() == Role.PARENT)
      students
          .findByParent_EmailOrderByClassNameAscRollNumberAsc(user.getEmail())
          .forEach(s -> classes.add(s.getClassName()));
    if (user.getRole() == Role.STUDENT)
      students
          .findByStudentAccount_Email(user.getEmail())
          .forEach(s -> classes.add(s.getClassName()));
    if (user.getRole() == Role.TEACHER)
      teacherClasses
          .findByTeacher_Email(user.getEmail())
          .forEach(t -> classes.add(t.getClassName()));
    List<Homework> result =
        user.getRole() == Role.ADMIN
            ? homework.findAll()
            : classes.stream()
                .flatMap(c -> homework.findByClassNameOrderByDueDateAsc(c).stream())
                .distinct()
                .toList();
    return result.stream().map(HomeworkView::from).toList();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  public HomeworkView create(
      @Valid @RequestBody HomeworkRequest r, @AuthenticationPrincipal Account user) {
    if (user.getRole() == Role.TEACHER
        && !teacherClasses.existsByTeacher_EmailAndClassName(user.getEmail(), r.className()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Class is outside your assignment");
    Homework h =
        homework.save(
            new Homework(
                r.title(), r.description(), r.className(), r.subject(), r.dueDate(), user));
    students.findAll().stream()
        .filter(s -> s.getClassName().equals(r.className()))
        .map(Student::getParent)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(
            p ->
                notifications.save(
                    new Notification(
                        p, "New homework", r.title() + " is due " + r.dueDate() + ".")));
    return HomeworkView.from(h);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!homework.existsById(id))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Homework not found");
    homework.deleteById(id);
  }
}
