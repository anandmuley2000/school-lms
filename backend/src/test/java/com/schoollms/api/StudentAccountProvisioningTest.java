package com.schoollms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import com.schoollms.domain.Student;
import com.schoollms.repository.AccountRepository;
import com.schoollms.repository.AttendanceRepository;
import com.schoollms.repository.FeeRepository;
import com.schoollms.repository.BusRouteRepository;
import com.schoollms.repository.ResultRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StudentAccountProvisioningTest {
  @Test
  void creatingStudentLinksToParentWithoutCreatingStudentLogin() {
    StudentRepository students = mock(StudentRepository.class);
    AccountRepository accounts = mock(AccountRepository.class);
    Account parent = new Account("parent@example.test", "hash", Role.PARENT, "Parent", null);
    when(students.findByStudentCode("STU099")).thenReturn(Optional.empty());
    when(accounts.findByEmail("parent@example.test")).thenReturn(Optional.of(parent));
    when(students.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));

    StudentController controller =
        new StudentController(
            students,
            accounts,
            mock(TeacherClassRepository.class),
            mock(AttendanceRepository.class),
            mock(ResultRepository.class),
            mock(FeeRepository.class),
            mock(BusRouteRepository.class));

    StudentView result =
        controller.create(
            new StudentRequest(
                "STU099", "Nila", "Patel", "6", "A", 9,
                "parent@example.test", "9876501234", null, "ACTIVE"));

    assertEquals("STU099", result.studentCode());
    verify(accounts, never()).save(any(Account.class));
    var studentCaptor = org.mockito.ArgumentCaptor.forClass(Student.class);
    verify(students).save(studentCaptor.capture());
    assertEquals(parent, studentCaptor.getValue().getParent());
    assertNull(studentCaptor.getValue().getStudentAccount());
  }
}
