package com.schoollms.api;

import com.schoollms.domain.BusRoute;
import com.schoollms.repository.BusRouteRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bus-routes")
@Transactional
public class BusRouteController {
  private final BusRouteRepository busRoutes;

  public BusRouteController(BusRouteRepository busRoutes) {
    this.busRoutes = busRoutes;
  }

  @GetMapping
  public List<BusRouteView> list() {
    return busRoutes.findAll().stream().map(BusRouteView::from).toList();
  }

  @GetMapping("/{id}")
  public BusRouteView getOne(@PathVariable Long id) {
    return BusRouteView.from(findRoute(id));
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
            request.latitude() != null ? request.latitude() : 18.5204,
            request.longitude() != null ? request.longitude() : 73.8567,
            request.speedKmvh() != null ? request.speedKmvh() : 35.0,
            request.currentStop() != null ? request.currentStop() : "School Campus",
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
      @PathVariable Long id, @Valid @RequestBody BusLocationUpdateRequest request) {
    BusRoute route = findRoute(id);
    route.updateLocation(request.latitude(), request.longitude(), request.speedKmvh(), request.currentStop());
    return BusRouteView.from(route);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    busRoutes.delete(findRoute(id));
  }

  private BusRoute findRoute(Long id) {
    return busRoutes
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus route not found"));
  }
}
