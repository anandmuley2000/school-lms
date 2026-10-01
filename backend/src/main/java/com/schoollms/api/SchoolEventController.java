package com.schoollms.api;

import com.schoollms.domain.Account;
import com.schoollms.domain.SchoolEvent;
import com.schoollms.repository.SchoolEventRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/events")
@Transactional
public class SchoolEventController {
  private final SchoolEventRepository events;

  public SchoolEventController(SchoolEventRepository events) {
    this.events = events;
  }

  @GetMapping
  public List<SchoolEventView> list() {
    return events.findAllByOrderByEventDateAsc().stream().map(SchoolEventView::from).toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public SchoolEventView create(
      @Valid @RequestBody SchoolEventRequest request, @AuthenticationPrincipal Account user) {
    return SchoolEventView.from(
        events.save(
            new SchoolEvent(
                request.title(),
                request.description(),
                request.eventDate(),
                request.location(),
                user)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public SchoolEventView update(
      @PathVariable Long id, @Valid @RequestBody SchoolEventRequest request) {
    SchoolEvent event = findEvent(id);
    event.update(request.title(), request.description(), request.eventDate(), request.location());
    return SchoolEventView.from(event);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    events.delete(findEvent(id));
  }

  private SchoolEvent findEvent(Long id) {
    return events
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
  }
}
