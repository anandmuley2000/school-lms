package com.schoollms.api;

import com.schoollms.domain.FeeStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record FeeRequest(
    @NotNull Long studentId,
    @NotBlank String description,
    @Positive double amount,
    LocalDate dueDate,
    FeeStatus status) {}
