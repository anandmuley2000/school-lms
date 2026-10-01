package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "fee_records")
public class FeeRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Student student;

  @NotBlank private String description;
  @Positive private double amount;
  private LocalDate dueDate;
  private LocalDate paidDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FeeStatus status = FeeStatus.PENDING;

  protected FeeRecord() {}

  public FeeRecord(Student student, String description, double amount, LocalDate dueDate) {
    this.student = student;
    this.description = description;
    this.amount = amount;
    this.dueDate = dueDate;
  }

  public Long getId() {
    return id;
  }

  public Student getStudent() {
    return student;
  }

  public String getDescription() {
    return description;
  }

  public double getAmount() {
    return amount;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public LocalDate getPaidDate() {
    return paidDate;
  }

  public FeeStatus getStatus() {
    return status;
  }

  public void setStatus(FeeStatus s) {
    status = s;
    paidDate = s == FeeStatus.PAID ? LocalDate.now() : null;
  }
}
