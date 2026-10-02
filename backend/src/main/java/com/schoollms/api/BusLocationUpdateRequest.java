package com.schoollms.api;

import jakarta.validation.constraints.NotNull;

public record BusLocationUpdateRequest(
    @NotNull Double latitude,
    @NotNull Double longitude,
    Double speedKmvh,
    String currentStop) {}
