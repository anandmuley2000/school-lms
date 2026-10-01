package com.schoollms.api;

import com.schoollms.domain.Attendance;
import java.time.LocalDate;

public record AttendanceView(
    Long id,
    Long studentId,
    String studentName,
    String studentCode,
    LocalDate date,
    String status) {
  public static AttendanceView from(Attendance a) {
    return new AttendanceView(
        a.getId(),
        a.getStudent().getId(),
        a.getStudent().getFullName(),
        a.getStudent().getStudentCode(),
        a.getDate(),
        a.getStatus().name());
  }
}
