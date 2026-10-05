package com.schoollms.api;

import com.schoollms.domain.BusRoute;
import com.schoollms.domain.*;
import com.schoollms.repository.BusRouteRepository;
import com.schoollms.repository.StudentRepository;
import com.schoollms.repository.TeacherClassRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bus-routes")
@Transactional
public class BusRouteController {
  private final BusRouteRepository busRoutes;
  private final StudentRepository students;
  private final TeacherClassRepository teacherClasses;

  public BusRouteController(BusRouteRepository busRoutes, StudentRepository students,
      TeacherClassRepository teacherClasses) {
    this.busRoutes = busRoutes;
    this.students = students;
    this.teacherClasses = teacherClasses;
  }

  @GetMapping
  public List<BusRouteView> list(@AuthenticationPrincipal Account user) {
    return busRoutes.findAll().stream().filter(route -> canView(route, user)).map(BusRouteView::from).toList();
  }

  @GetMapping("/{id}")
  public BusRouteView getOne(@PathVariable Long id, @AuthenticationPrincipal Account user) {
    BusRoute route = findRoute(id);
    if (!canView(route, user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bus is not assigned to your family or classes");
    return BusRouteView.from(route);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public BusRouteView create(@Valid @RequestBody BusRouteRequest request) {
    BusRoute route =
        new BusRoute(
            request.busNumber(),
            request.routeName(),
            request.driverName(),
            request.driverPhone(),
            request.capacity(),
            request.status(),
            request.latitude(),
            request.longitude(),
            request.speedKmvh(),
            request.currentStop(),
            request.stops());
    return BusRouteView.from(busRoutes.save(route));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public BusRouteView update(
      @PathVariable Long id, @Valid @RequestBody BusRouteRequest request) {
    BusRoute route = findRoute(id);
    route.updateDetails(
        request.busNumber(),
        request.routeName(),
        request.driverName(),
        request.driverPhone(),
        request.capacity(),
        request.status(),
        request.stops());
    if (request.latitude() != null || request.longitude() != null || request.speedKmvh() != null || request.currentStop() != null) {
      route.updateLocation(request.latitude(), request.longitude(), request.speedKmvh(), request.currentStop());
    }
    return BusRouteView.from(route);
  }

  @PutMapping("/{id}/location")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public BusRouteView updateLocation(
      @PathVariable Long id, @Valid @RequestBody BusLocationUpdateRequest request,
      @AuthenticationPrincipal Account user) {
    BusRoute route = findRoute(id);
    if (user.getRole() == Role.TEACHER && !canView(route, user))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bus is outside your assigned classes");
    route.updateLocation(request.latitude(), request.longitude(), request.speedKmvh(), request.currentStop());
    return BusRouteView.from(route);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    BusRoute route = findRoute(id);
    if (students.findAll().stream().anyMatch(student -> route.equals(student.getBusRoute())))
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Unassign the route from students before deleting it");
    busRoutes.delete(route);
  }

  private BusRoute findRoute(Long id) {
    return busRoutes
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus route not found"));
  }

  private boolean canView(BusRoute route, Account user) {
    if (user.getRole() == Role.ADMIN) return true;
    return students.findAll().stream().filter(s -> route.equals(s.getBusRoute())).anyMatch(s -> {
      if (user.getRole() == Role.PARENT) return s.getParent() != null && s.getParent().getId().equals(user.getId());
      return user.getRole() == Role.TEACHER && teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
          user.getEmail(), s.getClassName(), s.getDivision());
    });
  }
}
