package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "school_events")
public class SchoolEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String title;

  @Column(length = 4000)
  private String description;

  @NotNull
  @Column(nullable = false)
  private LocalDate eventDate;

  @Column(nullable = false)
  private boolean holiday;

  private String location;

  @ManyToOne(optional = false)
  private Account createdBy;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  protected SchoolEvent() {}

  public SchoolEvent(
      String title, String description, LocalDate eventDate, String location, Account createdBy) {
    this(title, description, eventDate, location, false, createdBy);
  }

  public SchoolEvent(
      String title, String description, LocalDate eventDate, String location, boolean holiday,
      Account createdBy) {
    update(title, description, eventDate, location, holiday);
    this.createdBy = createdBy;
  }

  public void update(
      String title, String description, LocalDate eventDate, String location, boolean holiday) {
    this.title = title;
    this.description = description;
    this.eventDate = eventDate;
    this.location = location;
    this.holiday = holiday;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public String getLocation() {
    return location;
  }

  public boolean isHoliday() {
    return holiday;
  }

  public String getCreatedBy() {
    return createdBy.getName();
  }
}
