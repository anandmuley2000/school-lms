package com.schoollms.api;

import com.schoollms.domain.Homework;
import java.time.LocalDate;

public record HomeworkView(
    Long id,
    String title,
    String description,
    String className,
    String division,
    String subject,
    LocalDate dueDate,
    String teacherName) {
  public static HomeworkView from(Homework h) {
    return new HomeworkView(
        h.getId(),
        h.getTitle(),
        h.getDescription(),
        h.getClassName(),
        h.getDivision(),
        h.getSubject(),
        h.getDueDate(),
        h.getTeacherName());
  }
}
