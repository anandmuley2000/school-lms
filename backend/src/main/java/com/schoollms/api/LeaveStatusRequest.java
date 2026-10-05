package com.schoollms.api;

import com.schoollms.domain.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveStatusRequest(@NotNull LeaveStatus status, @Size(max = 2000) String remark) {}
