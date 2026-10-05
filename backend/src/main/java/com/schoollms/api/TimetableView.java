package com.schoollms.api;

import com.schoollms.domain.ClassTimetable;
import java.time.LocalTime;

public record TimetableView(
    Long id, String academicYear, String className, String division, String dayOfWeek,
    String subject, Long teacherId, String teacherName, String room,
    LocalTime startTime, LocalTime endTime) {
  public static TimetableView from(ClassTimetable row) {
    return new TimetableView(row.getId(), row.getAcademicYear(), row.getClassName(), row.getDivision(),
        row.getDayOfWeek(), row.getSubject(), row.getTeacher().getId(), row.getTeacher().getName(),
        row.getRoom(), row.getStartTime(), row.getEndTime());
  }
}
