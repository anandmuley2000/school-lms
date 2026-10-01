package com.schoollms.api;

import com.schoollms.domain.Role;
import jakarta.validation.constraints.*;

public record AccountRequest(
    @Email @NotBlank String email,
    @NotBlank String name,
    String phone,
    @NotNull Role role,
    String password) {}
