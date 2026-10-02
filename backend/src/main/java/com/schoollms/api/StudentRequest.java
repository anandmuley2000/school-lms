package com.schoollms.api;

import jakarta.validation.constraints.*;

public record StudentRequest(
    @NotBlank String studentCode,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank String className,
    @NotBlank String division,
    @Positive int rollNumber,
    String parentEmail,
    String phone,
    String address,
    String status,
    @Email String studentLoginEmail,
    @Size(min = 8) String studentInitialPassword) {}
