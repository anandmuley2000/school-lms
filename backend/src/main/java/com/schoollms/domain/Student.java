package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "students", uniqueConstraints = @UniqueConstraint(columnNames = "student_code"))
public class Student {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "student_code", nullable = false)
  @NotBlank
  private String studentCode;

  @NotBlank private String firstName;
  @NotBlank private String lastName;
  @NotBlank private String className;
  @NotBlank private String division;
  @Positive private int rollNumber;
  private String gender;
  private LocalDate dateOfBirth;
  private String phone;
  private String address;
  private String status = "ACTIVE";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_account_id")
  private Account parent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_account_id")
  private Account studentAccount;

  protected Student() {}

  public Student(
      String studentCode,
      String firstName,
      String lastName,
      String className,
      String division,
      int rollNumber,
      Account parent) {
    this.studentCode = studentCode;
    this.firstName = firstName;
    this.lastName = lastName;
    this.className = className;
    this.division = division;
    this.rollNumber = rollNumber;
    this.parent = parent;
  }

  public Long getId() {
    return id;
  }

  public String getStudentCode() {
    return studentCode;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFullName() {
    return firstName + " " + lastName;
  }

  public String getClassName() {
    return className;
  }

  public String getDivision() {
    return division;
  }

  public int getRollNumber() {
    return rollNumber;
  }

  public String getStatus() {
    return status;
  }

  public String getPhone() {
    return phone;
  }

  public String getGender() {
    return gender;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public String getAddress() {
    return address;
  }

  public Account getParent() {
    return parent;
  }

  public Account getStudentAccount() {
    return studentAccount;
  }

  public void update(
      String firstName,
      String lastName,
      String className,
      String division,
      int rollNumber,
      String phone,
      String address,
      String status) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.className = className;
    this.division = division;
    this.rollNumber = rollNumber;
    this.phone = phone;
    this.address = address;
    this.status = status;
  }

  public void setParent(Account parent) {
    this.parent = parent;
  }

  public void setStudentAccount(Account account) {
    this.studentAccount = account;
  }
}
