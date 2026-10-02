package com.schoollms.api;

import com.schoollms.domain.BusRoute;
import java.time.Instant;

public record BusRouteView(
    Long id,
    String busNumber,
    String routeName,
    String driverName,
    String driverPhone,
    Integer capacity,
    String status,
    Double latitude,
    Double longitude,
    Double speedKmvh,
    String currentStop,
    String stops,
    Instant lastUpdated) {

  public static BusRouteView from(BusRoute route) {
    return new BusRouteView(
        route.getId(),
        route.getBusNumber(),
        route.getRouteName(),
        route.getDriverName(),
        route.getDriverPhone(),
        route.getCapacity(),
        route.getStatus(),
        route.getLatitude(),
        route.getLongitude(),
        route.getSpeedKmvh(),
        route.getCurrentStop(),
        route.getStops(),
        route.getLastUpdated());
  }
}
