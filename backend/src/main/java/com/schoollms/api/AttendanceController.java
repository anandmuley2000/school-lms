package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/attendance")
@Transactional
public class AttendanceController {
  private final AttendanceRepository attendance;
  private final StudentRepository students;
  private final NotificationRepository notifications;
  private final TeacherClassRepository teacherClasses;

  public AttendanceController(
      AttendanceRepository attendance,
      StudentRepository students,
      NotificationRepository notifications,
      TeacherClassRepository teacherClasses) {
    this.attendance = attendance;
    this.students = students;
    this.notifications = notifications;
    this.teacherClasses = teacherClasses;
  }

  @GetMapping("/student/{studentId}")
  public List<AttendanceView> history(
      @PathVariable Long studentId, @AuthenticationPrincipal Account user) {
    Student s =
        students
            .findById(studentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    boolean allowed =
        switch (user.getRole()) {
          case ADMIN -> true;
          case PARENT -> s.getParent() != null && s.getParent().getId().equals(user.getId());
          case STUDENT ->
              s.getStudentAccount() != null && s.getStudentAccount().getId().equals(user.getId());
          case TEACHER ->
              teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
                  user.getEmail(), s.getClassName(), s.getDivision());
        };
    if (!allowed)
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only access your own or assigned student records");
    return attendance.findByStudent_IdOrderByDateDesc(studentId).stream()
        .map(AttendanceView::from)
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public AttendanceView mark(
      @Valid @RequestBody AttendanceRequest r, @AuthenticationPrincipal Account user) {
    Student s =
        students
            .findById(r.studentId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    if (user.getRole() == Role.TEACHER
        && !teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
            user.getEmail(), s.getClassName(), s.getDivision()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Student is outside your assigned classes");
    Attendance a =
        attendance
            .findByStudent_IdAndDate(s.getId(), r.date())
            .orElseGet(() -> new Attendance(s, r.date(), r.status(), user));
    boolean newlyAbsent =
        r.status() == AttendanceStatus.ABSENT && a.getStatus() != AttendanceStatus.ABSENT;
    a.setStatus(r.status());
    a = attendance.save(a);
    if (newlyAbsent && s.getParent() != null) {
      String when = r.date().equals(java.time.LocalDate.now()) ? "today" : "on " + r.date();
      notifications.save(
          new Notification(
              s.getParent(),
              "Attendance Alert",
              "Attendance Alert: " + s.getFullName() + " was marked absent " + when + "."));
    }
    return AttendanceView.from(a);
  }
}
