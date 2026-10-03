package com.schoollms.config;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds stable demo rows only when explicitly enabled; synchronizes only its exact demo accounts. */
@Component
@ConditionalOnProperty(prefix = "app.demo", name = "data-enabled", havingValue = "true")
public class DemoAccountInitializer implements ApplicationRunner {
  private static final String DEMO_STUDENT_CODE = "DEMO-STU-001";
  private static final LocalDate DEMO_ATTENDANCE_DATE = LocalDate.of(2026, 10, 1);
  private static final LocalDate DEMO_EXAM_DATE = LocalDate.of(2026, 9, 25);

  private record DemoAccount(String email, String password, Role role, String name) {}

  private static final List<DemoAccount> DEMO_ACCOUNTS =
      List.of(
          new DemoAccount("admin@northstar.local", "Admin@123", Role.ADMIN, "System Administrator"),
          new DemoAccount("teacher@northstar.local", "Teacher@123", Role.TEACHER, "Demo Teacher"),
          new DemoAccount("parent@northstar.local", "Parent@123", Role.PARENT, "Demo Parent"),
          new DemoAccount("student@northstar.local", "Student@123", Role.STUDENT, "Demo Student"));

  private final AccountRepository accounts;
  private final StudentRepository students;
  private final TeacherClassRepository teacherClasses;
  private final AttendanceRepository attendance;
  private final HomeworkRepository homework;
  private final ResultRepository results;
  private final FeeRepository fees;
  private final SchoolEventRepository events;
  private final LeaveApplicationRepository leaveApplications;
  private final BusRouteRepository busRoutes;
  private final NotificationRepository notifications;
  private final MessageRepository messages;
  private final PasswordEncoder passwordEncoder;

  public DemoAccountInitializer(
      AccountRepository accounts,
      StudentRepository students,
      TeacherClassRepository teacherClasses,
      AttendanceRepository attendance,
      HomeworkRepository homework,
      ResultRepository results,
      FeeRepository fees,
      SchoolEventRepository events,
      LeaveApplicationRepository leaveApplications,
      BusRouteRepository busRoutes,
      NotificationRepository notifications,
      MessageRepository messages,
      PasswordEncoder passwordEncoder) {
    this.accounts = accounts;
    this.students = students;
    this.teacherClasses = teacherClasses;
    this.attendance = attendance;
    this.homework = homework;
    this.results = results;
    this.fees = fees;
    this.events = events;
    this.leaveApplications = leaveApplications;
    this.busRoutes = busRoutes;
    this.notifications = notifications;
    this.messages = messages;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Account admin = account("admin@northstar.local", "Admin@123", Role.ADMIN, "System Administrator");
    Account teacher = account("teacher@northstar.local", "Teacher@123", Role.TEACHER, "Demo Teacher");
    Account parent = account("parent@northstar.local", "Parent@123", Role.PARENT, "Demo Parent");
    Account studentLogin =
        account("student@northstar.local", "Student@123", Role.STUDENT, "Demo Student");

    Student student =
        students
            .findByStudentCode(DEMO_STUDENT_CODE)
            .orElseGet(
                () -> {
                  Student demoStudent =
                      new Student(DEMO_STUDENT_CODE, "Aarav", "Sharma", "6", "A", 1, parent);
                  demoStudent.setStudentAccount(studentLogin);
                  return students.save(demoStudent);
                });

    if (!teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
        teacher.getEmail(), "6", "A")) {
      teacherClasses.save(new TeacherClass(teacher, "6", "A"));
    }

    if (attendance.findByStudent_IdAndDate(student.getId(), DEMO_ATTENDANCE_DATE).isEmpty()) {
      attendance.save(
          new Attendance(student, DEMO_ATTENDANCE_DATE, AttendanceStatus.PRESENT, teacher));
    }

    if (!homework.existsByTeacher_IdAndClassNameAndTitle(
        teacher.getId(), "6", "Demo: Fractions review")) {
      homework.save(
          new Homework(
              "Demo: Fractions review",
              "Practice equivalent fractions and explain each solution.",
              "6",
              "Mathematics",
              LocalDate.now().plusDays(7),
              teacher));
    }
    if (!homework.existsByTeacher_IdAndClassNameAndTitle(
        teacher.getId(), "6", "Demo: Water cycle poster")) {
      homework.save(
          new Homework(
              "Demo: Water cycle poster",
              "Create a labeled diagram showing evaporation, condensation, and precipitation.",
              "6",
              "Science",
              LocalDate.now().plusDays(10),
              teacher));
    }

    if (!results.existsByStudent_IdAndExamNameAndSubject(
        student.getId(), "Demo: Term 1", "Mathematics")) {
      results.save(
          new AssessmentResult(
              student, "Demo: Term 1", "Mathematics", 82, 100, DEMO_EXAM_DATE, teacher));
    }
    if (!results.existsByStudent_IdAndExamNameAndSubject(
        student.getId(), "Demo: Term 1", "Science")) {
      results.save(
          new AssessmentResult(
              student, "Demo: Term 1", "Science", 88, 100, DEMO_EXAM_DATE, teacher));
    }

    String paidFeeDescription = "Demo: Term 1 tuition";
    if (!fees.existsByStudent_IdAndDescription(student.getId(), paidFeeDescription)) {
      FeeRecord paid =
          new FeeRecord(student, paidFeeDescription, 12500, DEMO_ATTENDANCE_DATE.plusDays(15));
      paid.setStatus(FeeStatus.PAID);
      fees.save(paid);
    }
    String pendingFeeDescription = "Demo: Term 2 tuition";
    if (!fees.existsByStudent_IdAndDescription(student.getId(), pendingFeeDescription)) {
      fees.save(
          new FeeRecord(student, pendingFeeDescription, 12500, DEMO_ATTENDANCE_DATE.plusDays(60)));
    }

    if (!events.existsByTitle("Demo: Annual Science Fair")) {
      events.save(
          new SchoolEvent(
              "Demo: Annual Science Fair",
              "Student projects and demonstrations for families.",
              LocalDate.now().plusDays(20),
              "School Auditorium",
              admin));
    }
    if (!events.existsByTitle("Demo: Parent-Teacher Meeting")) {
      events.save(
          new SchoolEvent(
              "Demo: Parent-Teacher Meeting",
              "A meeting to discuss student progress and next steps.",
              LocalDate.now().plusDays(28),
              "Classrooms",
              admin));
    }

    String leaveReason = "Demo: Family appointment";
    if (!leaveApplications.existsByRequester_IdAndStudent_IdAndReason(
        parent.getId(), student.getId(), leaveReason)) {
      leaveApplications.save(
          new LeaveApplication(
              parent,
              student,
              DEMO_ATTENDANCE_DATE.plusDays(20),
              DEMO_ATTENDANCE_DATE.plusDays(21),
              leaveReason));
    }

    if (busRoutes.findByBusNumber("DEMO-01").isEmpty()) {
      busRoutes.save(
          new BusRoute(
              "DEMO-01",
              "Demo Route - North District",
              "Demo Driver",
              "+1-202-555-0101",
              40,
              "IN_TRANSIT",
              40.7128,
              -74.0060,
              32.0,
              "Central Junction",
              "North Gate, Central Junction, East Avenue, Main Campus"));
    }

    String notificationTitle = "Demo: Welcome to School LMS";
    String notificationMessage = "Your demo account is ready. Explore your classes and updates here.";
    if (!notifications.existsByRecipient_IdAndTitleAndMessage(
        studentLogin.getId(), notificationTitle, notificationMessage)) {
      notifications.save(new Notification(studentLogin, notificationTitle, notificationMessage));
    }

    String messageSubject = "Demo: Welcome to the class";
    String messageBody = "Welcome! Please review the class updates and reach out with any questions.";
    if (!messages.existsBySender_IdAndRecipient_IdAndSubjectAndBody(
        teacher.getId(), parent.getId(), messageSubject, messageBody)) {
      messages.save(new SchoolMessage(teacher, parent, messageSubject, messageBody));
    }
  }

  private Account account(String email, String password, Role role, String name) {
    return accounts
        .findByEmailIgnoreCase(email)
        .map(
            existing -> {
              boolean passwordMatches = passwordEncoder.matches(password, existing.getPassword());
              if (!passwordMatches || existing.getRole() != role || !existing.isEnabled()) {
                String encodedPassword =
                    passwordMatches ? existing.getPassword() : passwordEncoder.encode(password);
                existing.synchronizeDemoCredentials(encodedPassword, role);
              }
              return existing;
            })
        .orElseGet(
            () -> accounts.save(new Account(email, passwordEncoder.encode(password), role, name, null)));
  }
}
