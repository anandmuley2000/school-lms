package com.schoollms.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BusRouteRequest(
    @NotBlank String busNumber,
    @NotBlank String routeName,
    @NotBlank String driverName,
    @NotBlank String driverPhone,
    @NotNull Integer capacity,
    @NotBlank String status,
    String stops,
    Double latitude,
    Double longitude,
    Double speedKmvh,
    String currentStop) {}
