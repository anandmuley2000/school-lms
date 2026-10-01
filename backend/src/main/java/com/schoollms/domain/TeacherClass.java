package com.schoollms.domain;

import jakarta.persistence.*;

@Entity
@Table(
    name = "teacher_classes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "class_name", "division"}))
public class TeacherClass {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "teacher_id")
  private Account teacher;

  @Column(name = "class_name", nullable = false)
  private String className;

  @Column(nullable = false)
  private String division;

  protected TeacherClass() {}

  public TeacherClass(Account teacher, String className, String division) {
    this.teacher = teacher;
    this.className = className;
    this.division = division;
  }

  public Long getId() {
    return id;
  }

  public String getClassName() {
    return className;
  }

  public String getDivision() {
    return division;
  }

  public String getTeacherEmail() {
    return teacher.getEmail();
  }
}
