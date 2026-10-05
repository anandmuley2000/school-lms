package com.schoollms.api;

import com.schoollms.domain.Student;

public record StudentView(
    Long id,
    String studentCode,
    String firstName,
    String lastName,
    String className,
    String division,
    int rollNumber,
    String phone,
    String address,
    String status,
    String parentName,
    String parentEmail,
    Long busRouteId) {
  public static StudentView from(Student s) {
    return new StudentView(
        s.getId(),
        s.getStudentCode(),
        s.getFirstName(),
        s.getLastName(),
        s.getClassName(),
        s.getDivision(),
        s.getRollNumber(),
        s.getPhone(),
        s.getAddress(),
        s.getStatus(),
        s.getParent() == null ? null : s.getParent().getName(),
        s.getParent() == null ? null : s.getParent().getEmail(),
        s.getBusRoute() == null ? null : s.getBusRoute().getId());
  }
}
