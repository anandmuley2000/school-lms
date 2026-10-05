package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_applications")
public class LeaveApplication {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Account requester;

  @ManyToOne
  private Student student;

  @NotNull
  @Column(nullable = false)
  private LocalDate startDate;

  @NotNull
  @Column(nullable = false)
  private LocalDate endDate;

  @NotBlank
  @Column(nullable = false, length = 2000)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LeaveStatus status = LeaveStatus.PENDING;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(length = 2000)
  private String reviewerRemark;

  private String reviewerName;

  private Instant reviewedAt;

  protected LeaveApplication() {}

  public LeaveApplication(
      Account requester, Student student, LocalDate startDate, LocalDate endDate, String reason) {
    this.requester = requester;
    update(student, startDate, endDate, reason);
  }

  public void update(Student student, LocalDate startDate, LocalDate endDate, String reason) {
    this.student = student;
    this.startDate = startDate;
    this.endDate = endDate;
    this.reason = reason;
  }

  public void setStatus(LeaveStatus status, Account reviewer) {
    this.status = status;
    this.reviewerName = reviewer.getName();
    this.reviewedAt = Instant.now();
  }

  public void setReviewerRemark(String reviewerRemark) {
    this.reviewerRemark = reviewerRemark == null || reviewerRemark.isBlank() ? null : reviewerRemark.trim();
  }

  public Long getId() {
    return id;
  }

  public Account getRequester() {
    return requester;
  }

  public Student getStudent() {
    return student;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getReason() {
    return reason;
  }

  public LeaveStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getReviewerRemark() {
    return reviewerRemark;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public String getReviewerName() {
    return reviewerName;
  }
}
