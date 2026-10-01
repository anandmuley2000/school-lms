package com.schoollms.api;

import com.schoollms.domain.*;
import java.time.LocalDate;

public record FeeView(
    Long id,
    Long studentId,
    String studentName,
    String description,
    double amount,
    LocalDate dueDate,
    LocalDate paidDate,
    String status) {
  public static FeeView from(FeeRecord f) {
    return new FeeView(
        f.getId(),
        f.getStudent().getId(),
        f.getStudent().getFullName(),
        f.getDescription(),
        f.getAmount(),
        f.getDueDate(),
        f.getPaidDate(),
        f.getStatus().name());
  }
}
