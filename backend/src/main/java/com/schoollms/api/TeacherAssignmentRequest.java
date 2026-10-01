package com.schoollms.api;

import jakarta.validation.constraints.NotBlank;

public record TeacherAssignmentRequest(@NotBlank String className, @NotBlank String division) {}
