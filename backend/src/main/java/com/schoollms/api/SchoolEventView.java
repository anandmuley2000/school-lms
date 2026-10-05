package com.schoollms.api;

import com.schoollms.domain.SchoolEvent;
import java.time.LocalDate;

public record SchoolEventView(
    Long id,
    String title,
    String description,
    LocalDate eventDate,
    String location,
    String createdBy,
    boolean holiday) {
  public static SchoolEventView from(SchoolEvent event) {
    return new SchoolEventView(
        event.getId(),
        event.getTitle(),
        event.getDescription(),
        event.getEventDate(),
        event.getLocation(),
        event.getCreatedBy(),
        HolidayClassifier.isHoliday(event.getTitle(), event.getDescription()));
  }
}
