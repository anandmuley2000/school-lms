package com.schoollms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.schoollms.domain.Account;
import com.schoollms.domain.ClassTimetable;
import com.schoollms.domain.Role;
import com.schoollms.domain.Student;
import com.schoollms.repository.AccountRepository;
import com.schoollms.repository.ClassTimetableRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimetableControllerTest {
  @Test
  void parentOnlyReceivesLinkedChildClassAndDivisionTimetable() {
    Account parent = new Account("parent@example.test", "hash", Role.PARENT, "Parent", null);
    Account teacher = new Account("teacher@example.test", "hash", Role.TEACHER, "Teacher", null);
    Student child = new Student("STU-A", "Ari", "Patel", "6", "A", 1, parent);
    ClassTimetable assigned = entry("6", "A", "Science", teacher);
    ClassTimetable unrelated = entry("6", "B", "Mathematics", teacher);
    ClassTimetableRepository timetable = mock(ClassTimetableRepository.class);
    StudentRepository students = mock(StudentRepository.class);
    when(timetable.findAllByOrderByDayOfWeekAscStartTimeAsc()).thenReturn(List.of(assigned, unrelated));
    when(students.findByParent_EmailOrderByClassNameAscRollNumberAsc(parent.getEmail()))
        .thenReturn(List.of(child));
    TimetableController controller = new TimetableController(timetable,
        mock(AccountRepository.class), students, mock(TeacherClassRepository.class));

    List<TimetableView> result = controller.list(parent);

    assertEquals(List.of("Science"), result.stream().map(TimetableView::subject).toList());
  }

  private static ClassTimetable entry(String className, String division, String subject, Account teacher) {
    return new ClassTimetable("2026-2027", className, division, "MONDAY", subject,
        teacher, "Room 1", LocalTime.of(9, 0), LocalTime.of(9, 45));
  }
}
