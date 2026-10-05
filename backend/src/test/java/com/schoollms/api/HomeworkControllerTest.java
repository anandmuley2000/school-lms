package com.schoollms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.schoollms.domain.Account;
import com.schoollms.domain.Homework;
import com.schoollms.domain.Role;
import com.schoollms.domain.Student;
import com.schoollms.repository.HomeworkRepository;
import com.schoollms.repository.NotificationRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeworkControllerTest {
  @Test
  void parentGetsChildDivisionHomeworkAndLegacyClassWideRows() {
    Account parent = new Account("parent@example.test", "hash", Role.PARENT, "Parent", null);
    Account teacher = new Account("teacher@example.test", "hash", Role.TEACHER, "Teacher", null);
    Student child = new Student("STU-1", "Ari", "Patel", "6", "A", 1, parent);
    Homework divisionA = new Homework("A task", "", "6", "A", "Math", LocalDate.now(), teacher);
    Homework divisionB = new Homework("B task", "", "6", "B", "Math", LocalDate.now(), teacher);
    Homework legacyClassWide = new Homework("Old task", "", "6", null, "Math", LocalDate.now(), teacher);

    StudentRepository students = mock(StudentRepository.class);
    HomeworkRepository homework = mock(HomeworkRepository.class);
    when(students.findByParent_EmailOrderByClassNameAscRollNumberAsc(parent.getEmail()))
        .thenReturn(List.of(child));
    when(homework.findAll()).thenReturn(List.of(divisionA, divisionB, legacyClassWide));

    HomeworkController controller = new HomeworkController(homework, students,
        mock(NotificationRepository.class), mock(TeacherClassRepository.class));

    assertEquals(List.of("A task", "Old task"),
        controller.list(parent).stream().map(HomeworkView::title).toList());
  }
}
