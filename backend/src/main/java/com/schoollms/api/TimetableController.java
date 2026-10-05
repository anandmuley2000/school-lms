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
@RequestMapping("/api/timetable")
@Transactional
public class TimetableController {
  private final ClassTimetableRepository timetable;
  private final AccountRepository accounts;
  private final StudentRepository students;
  private final TeacherClassRepository assignments;

  public TimetableController(ClassTimetableRepository timetable, AccountRepository accounts,
      StudentRepository students, TeacherClassRepository assignments) {
    this.timetable = timetable;
    this.accounts = accounts;
    this.students = students;
    this.assignments = assignments;
  }

  @GetMapping
  public List<TimetableView> list(@AuthenticationPrincipal Account user) {
    List<ClassTimetable> rows = timetable.findAllByOrderByDayOfWeekAscStartTimeAsc();
    if (user.getRole() == Role.ADMIN) return rows.stream().map(TimetableView::from).toList();
    if (user.getRole() == Role.TEACHER) {
      Set<String> scopes = new HashSet<>();
      assignments.findByTeacher_Email(user.getEmail())
          .forEach(a -> scopes.add(scope(a.getClassName(), a.getDivision())));
      rows = rows.stream().filter(row -> row.getTeacher().getId().equals(user.getId())
          || scopes.contains(scope(row.getClassName(), row.getDivision()))).toList();
    } else if (user.getRole() == Role.PARENT) {
      Set<String> scopes = new HashSet<>();
      students.findByParent_EmailOrderByClassNameAscRollNumberAsc(user.getEmail())
          .forEach(s -> scopes.add(scope(s.getClassName(), s.getDivision())));
      rows = rows.stream().filter(row -> scopes.contains(scope(row.getClassName(), row.getDivision()))).toList();
    } else {
      rows = List.of();
    }
    return rows.stream().map(TimetableView::from).toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public TimetableView create(@Valid @RequestBody TimetableRequest request) {
    validate(request, null);
    return TimetableView.from(timetable.save(new ClassTimetable(request.academicYear(),
        request.className(), request.division(), request.dayOfWeek(), request.subject(),
        teacher(request.teacherId()), request.room(), request.startTime(), request.endTime())));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public TimetableView update(@PathVariable Long id, @Valid @RequestBody TimetableRequest request) {
    ClassTimetable row = timetable.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Timetable entry not found"));
    validate(request, row.getId());
    row.update(request.academicYear(), request.className(), request.division(), request.dayOfWeek(),
        request.subject(), teacher(request.teacherId()), request.room(), request.startTime(), request.endTime());
    return TimetableView.from(row);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!timetable.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Timetable entry not found");
    timetable.deleteById(id);
  }

  private Account teacher(Long id) {
    Account teacher = accounts.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher account not found"));
    if (teacher.getRole() != Role.TEACHER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected account is not a teacher");
    return teacher;
  }

  private void validate(TimetableRequest request, Long ignoredId) {
    if (request.endTime().compareTo(request.startTime()) <= 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
    if (!Set.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        .contains(request.dayOfWeek().toUpperCase(Locale.ROOT)))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid day of week");
    String requestedDay = request.dayOfWeek().toUpperCase(Locale.ROOT);
    boolean collision = timetable.findAll().stream().filter(row -> !row.getId().equals(ignoredId))
        .filter(row -> row.getDayOfWeek().equalsIgnoreCase(requestedDay))
        .filter(row -> row.getStartTime().isBefore(request.endTime())
            && request.startTime().isBefore(row.getEndTime()))
        .anyMatch(row -> (row.getClassName().equals(request.className())
                && row.getDivision().equalsIgnoreCase(request.division()))
            || row.getTeacher().getId().equals(request.teacherId()));
    if (collision)
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "The class or teacher already has a period during that time");
  }

  private static String scope(String className, String division) { return className + "\u0000" + division; }
}
