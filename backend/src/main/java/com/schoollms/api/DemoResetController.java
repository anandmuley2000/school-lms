package com.schoollms.api;

import com.schoollms.repository.*;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/demo")
@PreAuthorize("hasRole('ADMIN')")
@Profile("development")
@ConditionalOnProperty(name = "app.demo.reset-enabled", havingValue = "true")
public class DemoResetController {
  private final List<CommandLineRunner> seeders;
  private final NotificationRepository notifications;
  private final MessageRepository messages;
  private final AttendanceRepository attendance;
  private final ResultRepository results;
  private final FeeRepository fees;
  private final HomeworkRepository homework;
  private final TeacherClassRepository teacherClasses;
  private final StudentRepository students;
  private final AccountRepository accounts;
  private final SchoolEventRepository events;
  private final LeaveApplicationRepository leaveApplications;
  private final BusRouteRepository busRoutes;

  public DemoResetController(
      List<CommandLineRunner> seeders,
      NotificationRepository notifications,
      MessageRepository messages,
      AttendanceRepository attendance,
      ResultRepository results,
      FeeRepository fees,
      HomeworkRepository homework,
      TeacherClassRepository teacherClasses,
      StudentRepository students,
      AccountRepository accounts,
      SchoolEventRepository events,
      LeaveApplicationRepository leaveApplications,
      BusRouteRepository busRoutes) {
    this.seeders = seeders;
    this.notifications = notifications;
    this.messages = messages;
    this.attendance = attendance;
    this.results = results;
    this.fees = fees;
    this.homework = homework;
    this.teacherClasses = teacherClasses;
    this.students = students;
    this.accounts = accounts;
    this.events = events;
    this.leaveApplications = leaveApplications;
    this.busRoutes = busRoutes;
  }

  @PostMapping("/reset")
  @Transactional
  public java.util.Map<String, String> reset() throws Exception {
    busRoutes.deleteAllInBatch();
    leaveApplications.deleteAllInBatch();
    events.deleteAllInBatch();
    notifications.deleteAllInBatch();
    messages.deleteAllInBatch();
    attendance.deleteAllInBatch();
    results.deleteAllInBatch();
    fees.deleteAllInBatch();
    homework.deleteAllInBatch();
    teacherClasses.deleteAllInBatch();
    students.deleteAllInBatch();
    accounts.deleteAllInBatch();
    for (CommandLineRunner seeder : seeders) seeder.run();
    return java.util.Map.of("message", "Demo data has been restored");
  }
}
