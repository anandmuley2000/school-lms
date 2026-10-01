package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final StudentRepository students;
  private final AccountRepository accounts;
  private final HomeworkRepository homework;
  private final AttendanceRepository attendance;
  private final FeeRepository fees;

  public DashboardController(
      StudentRepository s,
      AccountRepository a,
      HomeworkRepository h,
      AttendanceRepository at,
      FeeRepository fees) {
    students = s;
    accounts = a;
    homework = h;
    attendance = at;
    this.fees = fees;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, Long> summary() {
    LocalDate today = LocalDate.now();
    return Map.of(
        "totalStudents",
        students.count(),
        "totalTeachers",
        accounts.countByRole(Role.TEACHER),
        "totalParents",
        accounts.countByRole(Role.PARENT),
        "totalClasses",
        students.findAll().stream()
            .map(s -> s.getClassName() + "-" + s.getDivision())
            .distinct()
            .count(),
        "pendingHomework",
        homework.count(),
        "attendanceRecords",
        attendance.count(),
        "presentToday",
        attendance.countByDateAndStatus(today, AttendanceStatus.PRESENT),
        "absentToday",
        attendance.countByDateAndStatus(today, AttendanceStatus.ABSENT),
        "pendingFees",
        fees.countByStatus(FeeStatus.PENDING));
  }
}
