package com.schoollms.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SchoolEventRequest(
    @NotBlank @Size(max = 120) String title,
    @Size(max = 4000) String description,
    @NotNull LocalDate eventDate,
    @Size(max = 160) String location,
    boolean holiday) {}
