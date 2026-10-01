package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "attendance_date"}))
public class Attendance {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Student student;

  @Column(name = "attendance_date", nullable = false)
  @NotNull
  private LocalDate date;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AttendanceStatus status;

  @ManyToOne(optional = false)
  private Account markedBy;

  protected Attendance() {}

  public Attendance(Student student, LocalDate date, AttendanceStatus status, Account markedBy) {
    this.student = student;
    this.date = date;
    this.status = status;
    this.markedBy = markedBy;
  }

  public Long getId() {
    return id;
  }

  public Student getStudent() {
    return student;
  }

  public LocalDate getDate() {
    return date;
  }

  public AttendanceStatus getStatus() {
    return status;
  }

  public void setStatus(AttendanceStatus status) {
    this.status = status;
  }
}
