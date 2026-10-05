package com.schoollms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.schoollms.domain.Account;
import com.schoollms.domain.LeaveApplication;
import com.schoollms.domain.LeaveStatus;
import com.schoollms.domain.Role;
import com.schoollms.domain.Student;
import com.schoollms.repository.LeaveApplicationRepository;
import com.schoollms.repository.NotificationRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class LeaveApplicationControllerTest {
  @Test
  void teacherSeesOwnAndOnlyAssignedStudentLeave() {
    Account teacher = account("teacher@example.test", Role.TEACHER, "Teacher", 1L);
    Account parent = account("parent@example.test", Role.PARENT, "Parent", 2L);
    Student assigned = new Student("STU-A", "Ari", "Patel", "6", "A", 1, parent);
    Student unrelated = new Student("STU-B", "Bea", "Shah", "7", "B", 2, parent);
    List<LeaveApplication> rows = List.of(
        leave(teacher, null), leave(parent, assigned), leave(parent, unrelated));
    LeaveApplicationRepository applications = mock(LeaveApplicationRepository.class);
    TeacherClassRepository assignments = mock(TeacherClassRepository.class);
    when(applications.findAllByOrderByCreatedAtDesc()).thenReturn(rows);
    when(assignments.existsByTeacher_EmailAndClassNameAndDivision("teacher@example.test", "6", "A"))
        .thenReturn(true);
    when(assignments.existsByTeacher_EmailAndClassNameAndDivision("teacher@example.test", "7", "B"))
        .thenReturn(false);
    LeaveApplicationController controller = controller(applications, assignments,
        mock(StudentRepository.class), mock(NotificationRepository.class));

    List<LeaveApplicationView> result = controller.list(teacher);

    assertEquals(2, result.size());
  }

  @Test
  void teacherCannotReviewOwnStaffLeave() {
    Account teacher = account("teacher@example.test", Role.TEACHER, "Teacher", 1L);
    LeaveApplication ownRequest = leave(teacher, null);
    LeaveApplicationRepository applications = mock(LeaveApplicationRepository.class);
    NotificationRepository notifications = mock(NotificationRepository.class);
    when(applications.findById(10L)).thenReturn(Optional.of(ownRequest));
    LeaveApplicationController controller = controller(applications,
        mock(TeacherClassRepository.class), mock(StudentRepository.class), notifications);

    ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
        controller.updateStatus(10L, new LeaveStatusRequest(LeaveStatus.APPROVED, "Not allowed"), teacher));

    assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    verify(notifications, never()).save(any());
  }

  @Test
  void assignedTeacherCanRecordStudentLeaveDecisionAndRemark() {
    Account teacher = account("teacher@example.test", Role.TEACHER, "Teacher", 1L);
    Account parent = account("parent@example.test", Role.PARENT, "Parent", 2L);
    Student student = new Student("STU-A", "Ari", "Patel", "6", "A", 1, parent);
    LeaveApplication request = leave(parent, student);
    LeaveApplicationRepository applications = mock(LeaveApplicationRepository.class);
    TeacherClassRepository assignments = mock(TeacherClassRepository.class);
    when(applications.findById(11L)).thenReturn(Optional.of(request));
    when(assignments.existsByTeacher_EmailAndClassNameAndDivision("teacher@example.test", "6", "A"))
        .thenReturn(true);
    LeaveApplicationController controller = controller(applications, assignments,
        mock(StudentRepository.class), mock(NotificationRepository.class));

    LeaveApplicationView result = controller.updateStatus(11L,
        new LeaveStatusRequest(LeaveStatus.REJECTED, "Please provide a medical note."), teacher);

    assertEquals(LeaveStatus.REJECTED, result.status());
    assertEquals("Please provide a medical note.", result.reviewerRemark());
    org.junit.jupiter.api.Assertions.assertNotNull(result.reviewedAt());
  }

  private static LeaveApplicationController controller(LeaveApplicationRepository applications,
      TeacherClassRepository assignments, StudentRepository students,
      NotificationRepository notifications) {
    return new LeaveApplicationController(applications, students, notifications, assignments);
  }

  private static Account account(String email, Role role, String name, long id) {
    Account account = new Account(email, "hash", role, name, null);
    ReflectionTestUtils.setField(account, "id", id);
    return account;
  }

  private static LeaveApplication leave(Account requester, Student student) {
    return new LeaveApplication(requester, student, LocalDate.of(2026, 10, 6),
        LocalDate.of(2026, 10, 7), "Family commitment");
  }
}
