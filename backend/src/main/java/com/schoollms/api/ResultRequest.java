package com.schoollms.api;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ResultRequest(
    @NotNull Long studentId,
    @NotBlank String examName,
    @NotBlank String subject,
    @PositiveOrZero double marks,
    @Positive double maximumMarks,
    LocalDate examDate) {}
