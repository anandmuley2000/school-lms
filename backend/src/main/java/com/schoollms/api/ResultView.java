package com.schoollms.api;

import com.schoollms.domain.AssessmentResult;
import java.time.LocalDate;

public record ResultView(
    Long id,
    Long studentId,
    String studentName,
    String studentCode,
    String examName,
    String subject,
    double marks,
    double maximumMarks,
    double percentage,
    LocalDate examDate) {
  public static ResultView from(AssessmentResult r) {
    return new ResultView(
        r.getId(),
        r.getStudent().getId(),
        r.getStudent().getFullName(),
        r.getStudent().getStudentCode(),
        r.getExamName(),
        r.getSubject(),
        r.getMarks(),
        r.getMaximumMarks(),
        Math.round(r.getMarks() / r.getMaximumMarks() * 1000.0) / 10.0,
        r.getExamDate());
  }
}
