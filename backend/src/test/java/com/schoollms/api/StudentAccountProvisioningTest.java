package com.schoollms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import com.schoollms.domain.Student;
import com.schoollms.repository.AccountRepository;
import com.schoollms.repository.AttendanceRepository;
import com.schoollms.repository.FeeRepository;
import com.schoollms.repository.ResultRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class StudentAccountProvisioningTest {
  @Test
  void creatingStudentCreatesAndLinksHashedStudentLogin() {
    StudentRepository students = mock(StudentRepository.class);
    AccountRepository accounts = mock(AccountRepository.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    when(students.findByStudentCode("STU099")).thenReturn(Optional.empty());
    when(accounts.findByEmail("learner@example.test")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("FirstLogin!234")).thenReturn("bcrypt-hash");
    when(accounts.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));
    when(students.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));

    StudentController controller =
        new StudentController(
            students,
            accounts,
            mock(TeacherClassRepository.class),
            mock(AttendanceRepository.class),
            mock(ResultRepository.class),
            mock(FeeRepository.class),
            passwordEncoder);

    StudentView result =
        controller.create(
            new StudentRequest(
                "STU099",
                "Nila",
                "Patel",
                "6",
                "A",
                9,
                null,
                "9876501234",
                null,
                "ACTIVE",
                "learner@example.test",
                "FirstLogin!234"));

    verify(passwordEncoder).encode("FirstLogin!234");
    verify(accounts).save(any(Account.class));
    assertEquals("STU099", result.studentCode());

    var studentCaptor = org.mockito.ArgumentCaptor.forClass(Student.class);
    verify(students).save(studentCaptor.capture());
    Account linkedAccount = studentCaptor.getValue().getStudentAccount();
    assertNotNull(linkedAccount);
    assertEquals("learner@example.test", linkedAccount.getEmail());
    assertEquals(Role.STUDENT, linkedAccount.getRole());
    assertEquals("bcrypt-hash", linkedAccount.getPassword());
  }
}
