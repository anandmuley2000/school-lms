package com.schoollms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.schoollms.domain.Account;
import com.schoollms.domain.BusRoute;
import com.schoollms.domain.Role;
import com.schoollms.domain.Student;
import com.schoollms.repository.BusRouteRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BusRouteControllerTest {
  @Test
  void parentOnlySeesBusAssignedToLinkedChild() {
    Account parent = new Account("parent@example.test", "hash", Role.PARENT, "Parent", null);
    ReflectionTestUtils.setField(parent, "id", 1L);
    BusRoute assigned = route("BUS-1", "North route");
    BusRoute unrelated = route("BUS-2", "South route");
    Student child = new Student("STU-A", "Ari", "Patel", "6", "A", 1, parent);
    child.setBusRoute(assigned);
    BusRouteRepository routes = mock(BusRouteRepository.class);
    StudentRepository students = mock(StudentRepository.class);
    when(routes.findAll()).thenReturn(List.of(assigned, unrelated));
    when(students.findAll()).thenReturn(List.of(child));
    BusRouteController controller = new BusRouteController(routes, students,
        mock(TeacherClassRepository.class));

    List<BusRouteView> visible = controller.list(parent);

    assertEquals(List.of("BUS-1"), visible.stream().map(BusRouteView::busNumber).toList());
  }

  private static BusRoute route(String number, String name) {
    return new BusRoute(number, name, "Driver", "555-0100", 40, "ACTIVE", null, null,
        null, null, "Main Gate, Central Junction");
  }
}
