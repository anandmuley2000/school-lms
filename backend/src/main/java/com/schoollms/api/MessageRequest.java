package com.schoollms.api;

import jakarta.validation.constraints.*;

public record MessageRequest(
    @Email @NotBlank String recipientEmail,
    @NotBlank String subject,
    @NotBlank @Size(max = 5000) String body) {}
