package com.schoollms.api;

import jakarta.validation.constraints.*;

public record StudentRequest(
    @NotBlank String studentCode,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank String className,
    @NotBlank String division,
    @Positive int rollNumber,
    @NotBlank @Email String parentEmail,
    String phone,
    String address,
    String status,
    Long busRouteId) {
  public StudentRequest(String studentCode, String firstName, String lastName, String className,
      String division, int rollNumber, String parentEmail, String phone, String address, String status) {
    this(studentCode, firstName, lastName, className, division, rollNumber, parentEmail, phone, address, status, null);
  }
}
