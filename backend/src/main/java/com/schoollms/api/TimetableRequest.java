package com.schoollms.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record TimetableRequest(
    @NotBlank String academicYear,
    @NotBlank String className,
    @NotBlank String division,
    @NotBlank String dayOfWeek,
    @NotBlank String subject,
    @NotNull Long teacherId,
    @NotBlank String room,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime) {}
