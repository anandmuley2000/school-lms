package com.schoollms.api;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record HomeworkRequest(
    @NotBlank String title,
    String description,
    @NotBlank String className,
    @NotBlank String division,
    String subject,
    LocalDate dueDate) {}
