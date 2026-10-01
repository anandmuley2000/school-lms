package com.schoollms.api;

import com.schoollms.domain.LeaveStatus;
import jakarta.validation.constraints.NotNull;

public record LeaveStatusRequest(@NotNull LeaveStatus status) {}
