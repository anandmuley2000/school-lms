package com.schoollms.api;

import com.schoollms.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AttendanceRequest(
    @NotNull Long studentId, @NotNull LocalDate date, @NotNull AttendanceStatus status) {}
