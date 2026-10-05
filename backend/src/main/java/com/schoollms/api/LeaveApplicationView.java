package com.schoollms.api;

import com.schoollms.domain.LeaveApplication;
import com.schoollms.domain.Student;
import com.schoollms.domain.LeaveStatus;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveApplicationView(
    Long id,
    Long studentId,
    String studentName,
    String requesterName,
    String requesterEmail,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    LeaveStatus status,
    Instant createdAt,
    String reviewerRemark,
    Instant reviewedAt) {
  public static LeaveApplicationView from(LeaveApplication application) {
    Student student = application.getStudent();
    return new LeaveApplicationView(
        application.getId(),
        student == null ? null : student.getId(),
        student == null ? null : student.getFullName(),
        application.getRequester().getName(),
        application.getRequester().getEmail(),
        application.getStartDate(),
        application.getEndDate(),
        application.getReason(),
        application.getStatus(),
        application.getCreatedAt(),
        application.getReviewerRemark(),
        application.getReviewedAt());
  }
}
