package com.schoollms.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class DemoAccountInitializerTest {
  private static final List<String[]> DEMO_LOGINS =
      List.<String[]>of(
          new String[] {"admin@northstar.local", "Admin@123", "System Administrator", "ADMIN"},
          new String[] {"teacher@northstar.local", "Teacher@123", "Demo Teacher", "TEACHER"},
          new String[] {"parent@northstar.local", "Parent@123", "Demo Parent", "PARENT"},
          new String[] {"student@northstar.local", "Student@123", "Demo Student", "STUDENT"});

  private AccountRepository accounts;
  private StudentRepository students;
  private TeacherClassRepository teacherClasses;
  private AttendanceRepository attendance;
  private HomeworkRepository homework;
  private ResultRepository results;
  private FeeRepository fees;
  private SchoolEventRepository events;
  private LeaveApplicationRepository leaveApplications;
  private BusRouteRepository busRoutes;
  private NotificationRepository notifications;
  private MessageRepository messages;
  private PasswordEncoder passwordEncoder;
  private DemoAccountInitializer initializer;

  @BeforeEach
  void setUp() {
    accounts = mock(AccountRepository.class);
    students = mock(StudentRepository.class);
    teacherClasses = mock(TeacherClassRepository.class);
    attendance = mock(AttendanceRepository.class);
    homework = mock(HomeworkRepository.class);
    results = mock(ResultRepository.class);
    fees = mock(FeeRepository.class);
    events = mock(SchoolEventRepository.class);
    leaveApplications = mock(LeaveApplicationRepository.class);
    busRoutes = mock(BusRouteRepository.class);
    notifications = mock(NotificationRepository.class);
    messages = mock(MessageRepository.class);
    passwordEncoder = new SecurityConfig().passwordEncoder();
    initializer =
        new DemoAccountInitializer(
            accounts,
            students,
            teacherClasses,
            attendance,
            homework,
            results,
            fees,
            events,
            leaveApplications,
            busRoutes,
            notifications,
            messages,
            passwordEncoder);

    when(accounts.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(students.findByStudentCode("DEMO-STU-001")).thenReturn(Optional.empty());
    when(students.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(attendance.findByStudent_IdAndDate(null, LocalDate.of(2026, 10, 1)))
        .thenReturn(Optional.empty());
    when(busRoutes.findByBusNumber("DEMO-01")).thenReturn(Optional.empty());
    when(busRoutes.save(any(BusRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

    when(teacherClasses.save(any(TeacherClass.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(attendance.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(homework.save(any(Homework.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(results.save(any(AssessmentResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(fees.save(any(FeeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(events.save(any(SchoolEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(leaveApplications.save(any(LeaveApplication.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(notifications.save(any(Notification.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(messages.save(any(SchoolMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void createsHashedAccountsAndRealisticRelatedRowsForSupportedTables() throws Exception {
    initializer.run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<Account> savedAccounts = ArgumentCaptor.forClass(Account.class);
    verify(accounts, times(4)).save(savedAccounts.capture());
    for (int i = 0; i < DEMO_LOGINS.size(); i++) {
      Account account = savedAccounts.getAllValues().get(i);
      String[] login = DEMO_LOGINS.get(i);
      assertEquals(login[0], account.getEmail());
      assertEquals(login[2], account.getName());
      assertEquals(login[3], account.getRole().name());
      assertTrue(account.isEnabled());
      assertNotEquals(login[1], account.getPassword());
      assertTrue(account.getPassword().startsWith("$2"));
      assertTrue(passwordEncoder.matches(login[1], account.getPassword()));
      when(accounts.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
    }

    authenticateDemoLogins();
    ArgumentCaptor<Student> seededStudent = ArgumentCaptor.forClass(Student.class);
    verify(students).save(seededStudent.capture());
    verify(teacherClasses).save(any(TeacherClass.class));
    verify(attendance).save(any(Attendance.class));
    verify(homework, times(2)).save(any(Homework.class));
    verify(results, times(2)).save(any(AssessmentResult.class));
    verify(fees, times(2)).save(any(FeeRecord.class));
    verify(events, times(2)).save(any(SchoolEvent.class));
    verify(leaveApplications).save(any(LeaveApplication.class));
    verify(busRoutes).save(any(BusRoute.class));
    verify(notifications).save(any(Notification.class));
    verify(messages).save(any(SchoolMessage.class));

    assertEquals("DEMO-STU-001", seededStudent.getValue().getStudentCode());
    assertEquals("parent@northstar.local", seededStudent.getValue().getParent().getEmail());
    assertEquals("student@northstar.local", seededStudent.getValue().getStudentAccount().getEmail());
  }

  @Test
  void doesNotSaveAgainWhenSeedNaturalKeysAlreadyExist() throws Exception {
    initializer.run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<Account> savedAccounts = ArgumentCaptor.forClass(Account.class);
    ArgumentCaptor<Student> savedStudents = ArgumentCaptor.forClass(Student.class);
    ArgumentCaptor<Attendance> savedAttendance = ArgumentCaptor.forClass(Attendance.class);
    ArgumentCaptor<BusRoute> savedBus = ArgumentCaptor.forClass(BusRoute.class);
    verify(accounts, times(4)).save(savedAccounts.capture());
    verify(students).save(savedStudents.capture());
    verify(attendance).save(savedAttendance.capture());
    verify(busRoutes).save(savedBus.capture());

    for (Account account : savedAccounts.getAllValues()) {
      when(accounts.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
    }
    Student student = savedStudents.getValue();
    when(students.findByStudentCode("DEMO-STU-001")).thenReturn(Optional.of(student));
    when(teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
            "teacher@northstar.local", "6", "A"))
        .thenReturn(true);
    when(attendance.findByStudent_IdAndDate(student.getId(), LocalDate.of(2026, 10, 1)))
        .thenReturn(Optional.of(savedAttendance.getValue()));
    when(homework.existsByTeacher_IdAndClassNameAndTitle(
            null, "6", "Demo: Fractions review"))
        .thenReturn(true);
    when(homework.existsByTeacher_IdAndClassNameAndTitle(
            null, "6", "Demo: Water cycle poster"))
        .thenReturn(true);
    when(results.existsByStudent_IdAndExamNameAndSubject(null, "Demo: Term 1", "Mathematics"))
        .thenReturn(true);
    when(results.existsByStudent_IdAndExamNameAndSubject(null, "Demo: Term 1", "Science"))
        .thenReturn(true);
    when(fees.existsByStudent_IdAndDescription(null, "Demo: Term 1 tuition")).thenReturn(true);
    when(fees.existsByStudent_IdAndDescription(null, "Demo: Term 2 tuition")).thenReturn(true);
    when(events.existsByTitle("Demo: Annual Science Fair")).thenReturn(true);
    when(events.existsByTitle("Demo: Parent-Teacher Meeting")).thenReturn(true);
    when(leaveApplications.existsByRequester_IdAndStudent_IdAndReason(
            null, student.getId(), "Demo: Family appointment"))
        .thenReturn(true);
    when(busRoutes.findByBusNumber("DEMO-01")).thenReturn(Optional.of(savedBus.getValue()));
    when(notifications.existsByRecipient_IdAndTitleAndMessage(
            null,
            "Demo: Welcome to School LMS",
            "Your demo account is ready. Explore your classes and updates here."))
        .thenReturn(true);
    when(messages.existsBySender_IdAndRecipient_IdAndSubjectAndBody(
            null,
            null,
            "Demo: Welcome to the class",
            "Welcome! Please review the class updates and reach out with any questions."))
        .thenReturn(true);

    clearInvocations(
        accounts,
        students,
        teacherClasses,
        attendance,
        homework,
        results,
        fees,
        events,
        leaveApplications,
        busRoutes,
        notifications,
        messages);
    initializer.run(new DefaultApplicationArguments(new String[0]));

    verifyNoSaves();
  }

  @Test
  void synchronizesExistingDemoAccountsWithoutDuplicatesAndLeavesNonDemoAccountsUntouched()
      throws Exception {
    Role[] roles = Role.values();
    List<Account> existingDemoAccounts = new java.util.ArrayList<>();
    for (int i = 0; i < DEMO_LOGINS.size(); i++) {
      String[] login = DEMO_LOGINS.get(i);
      Account existing =
          new Account(
              login[0],
              passwordEncoder.encode("StalePassword@123"),
              roles[(i + 1) % roles.length],
              "Keep This Name " + i,
              "555-010" + i);
      ReflectionTestUtils.setField(existing, "id", 100L + i);
      ReflectionTestUtils.setField(existing, "active", false);
      existingDemoAccounts.add(existing);
      when(accounts.findByEmailIgnoreCase(login[0])).thenReturn(Optional.of(existing));
    }

    String unrelatedPassword = "UnrelatedPassword@123";
    Account unrelated =
        new Account(
            "unrelated@northstar.local",
            passwordEncoder.encode(unrelatedPassword),
            Role.PARENT,
            "Unrelated Account",
            "555-0199");
    ReflectionTestUtils.setField(unrelated, "id", 999L);
    ReflectionTestUtils.setField(unrelated, "active", false);

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(accounts, never()).save(any(Account.class));
    for (int i = 0; i < DEMO_LOGINS.size(); i++) {
      String[] login = DEMO_LOGINS.get(i);
      Account existing = existingDemoAccounts.get(i);
      assertEquals(100L + i, existing.getId());
      assertEquals("Keep This Name " + i, existing.getName());
      assertEquals("555-010" + i, existing.getPhone());
      assertEquals(login[3], existing.getRole().name());
      assertTrue(existing.isEnabled());
      assertTrue(passwordEncoder.matches(login[1], existing.getPassword()));
      assertFalse(passwordEncoder.matches("StalePassword@123", existing.getPassword()));
    }

    authenticateDemoLogins();

    assertEquals(999L, unrelated.getId());
    assertEquals("unrelated@northstar.local", unrelated.getEmail());
    assertEquals("Unrelated Account", unrelated.getName());
    assertEquals(Role.PARENT, unrelated.getRole());
    assertFalse(unrelated.isEnabled());
    assertTrue(passwordEncoder.matches(unrelatedPassword, unrelated.getPassword()));
    verify(accounts, never()).findByEmailIgnoreCase("unrelated@northstar.local");
  }

  private void authenticateDemoLogins() {
    DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(new SecurityConfig().userDetailsService(accounts));
    provider.setPasswordEncoder(passwordEncoder);
    AuthenticationManager manager = new ProviderManager(provider);

    for (String[] login : DEMO_LOGINS) {
      assertTrue(
          manager
              .authenticate(new UsernamePasswordAuthenticationToken(login[0], login[1]))
              .isAuthenticated());
    }
  }

  private void verifyNoSaves() {
    verify(accounts, never()).save(any(Account.class));
    verify(students, never()).save(any(Student.class));
    verify(teacherClasses, never()).save(any(TeacherClass.class));
    verify(attendance, never()).save(any(Attendance.class));
    verify(homework, never()).save(any(Homework.class));
    verify(results, never()).save(any(AssessmentResult.class));
    verify(fees, never()).save(any(FeeRecord.class));
    verify(events, never()).save(any(SchoolEvent.class));
    verify(leaveApplications, never()).save(any(LeaveApplication.class));
    verify(busRoutes, never()).save(any(BusRoute.class));
    verify(notifications, never()).save(any(Notification.class));
    verify(messages, never()).save(any(SchoolMessage.class));
  }
}
