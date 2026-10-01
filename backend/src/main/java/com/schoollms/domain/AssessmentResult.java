package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "assessment_results")
public class AssessmentResult {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Student student;

  @NotBlank private String examName;
  @NotBlank private String subject;
  @PositiveOrZero private double marks;
  @Positive private double maximumMarks;
  private LocalDate examDate;

  @ManyToOne(optional = false)
  private Account enteredBy;

  protected AssessmentResult() {}

  public AssessmentResult(
      Student student,
      String examName,
      String subject,
      double marks,
      double maximumMarks,
      LocalDate examDate,
      Account enteredBy) {
    this.student = student;
    this.examName = examName;
    this.subject = subject;
    this.marks = marks;
    this.maximumMarks = maximumMarks;
    this.examDate = examDate;
    this.enteredBy = enteredBy;
  }

  public Long getId() {
    return id;
  }

  public Student getStudent() {
    return student;
  }

  public String getExamName() {
    return examName;
  }

  public String getSubject() {
    return subject;
  }

  public double getMarks() {
    return marks;
  }

  public double getMaximumMarks() {
    return maximumMarks;
  }

  public LocalDate getExamDate() {
    return examDate;
  }
}
