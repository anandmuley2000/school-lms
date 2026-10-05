package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Entity
@Table(name = "class_timetable")
public class ClassTimetable {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

  @Column(nullable = false, length = 20) @NotBlank private String academicYear;
  @Column(nullable = false, length = 40) @NotBlank private String className;
  @Column(nullable = false, length = 20) @NotBlank private String division;
  @Column(nullable = false, length = 12) @NotBlank private String dayOfWeek;
  @Column(nullable = false, length = 100) @NotBlank private String subject;
  @ManyToOne(optional = false, fetch = FetchType.LAZY) private Account teacher;
  @Column(nullable = false, length = 80) @NotBlank private String room;
  @Column(nullable = false) @NotNull private LocalTime startTime;
  @Column(nullable = false) @NotNull private LocalTime endTime;

  protected ClassTimetable() {}

  public ClassTimetable(String academicYear, String className, String division, String dayOfWeek,
      String subject, Account teacher, String room, LocalTime startTime, LocalTime endTime) {
    update(academicYear, className, division, dayOfWeek, subject, teacher, room, startTime, endTime);
  }

  public void update(String academicYear, String className, String division, String dayOfWeek,
      String subject, Account teacher, String room, LocalTime startTime, LocalTime endTime) {
    this.academicYear = academicYear;
    this.className = className;
    this.division = division;
    this.dayOfWeek = dayOfWeek;
    this.subject = subject;
    this.teacher = teacher;
    this.room = room;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public Long getId() { return id; }
  public String getAcademicYear() { return academicYear; }
  public String getClassName() { return className; }
  public String getDivision() { return division; }
  public String getDayOfWeek() { return dayOfWeek; }
  public String getSubject() { return subject; }
  public Account getTeacher() { return teacher; }
  public String getRoom() { return room; }
  public LocalTime getStartTime() { return startTime; }
  public LocalTime getEndTime() { return endTime; }
}
