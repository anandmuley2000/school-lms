package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "homework")
public class Homework {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank private String title;

  @Column(length = 4000)
  private String description;

  @NotBlank private String className;
  @Column(name = "division")
  private String division;
  private String subject;
  private LocalDate dueDate;

  @ManyToOne(optional = false)
  private Account teacher;

  private Instant createdAt = Instant.now();

  protected Homework() {}

  public Homework(
      String title,
      String description,
      String className,
      String division,
      String subject,
      LocalDate dueDate,
      Account teacher) {
    this.title = title;
    this.description = description;
    this.className = className;
    this.division = division;
    this.subject = subject;
    this.dueDate = dueDate;
    this.teacher = teacher;
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

  public String getClassName() {
    return className;
  }

  public String getDivision() {
    return division;
  }

  public String getSubject() {
    return subject;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getTeacherName() {
    return teacher.getName();
  }
}
