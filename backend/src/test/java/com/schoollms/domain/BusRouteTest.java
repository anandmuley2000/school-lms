package com.schoollms.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BusRouteTest {
  @Test
  void newRouteDoesNotInventLocationOrSpeedBeforeGpsIsReported() {
    BusRoute route =
        new BusRoute(
            "BUS-100",
            "North Route",
            "Driver",
            "555-0100",
            40,
            "INACTIVE",
            null,
            null,
            null,
            "",
            "North Gate, School");

    assertNull(route.getLatitude());
    assertNull(route.getLongitude());
    assertNull(route.getSpeedKmvh());
    assertNull(route.getCurrentStop());
    assertEquals("INACTIVE", route.getStatus());
    assertNotNull(route.getLastUpdated());
  }

  @Test
  void locationUpdatesStoreOnlyTheCoordinatesActuallyReported() {
    BusRoute route =
        new BusRoute(
            "BUS-100",
            "North Route",
            "Driver",
            "555-0100",
            40,
            "INACTIVE",
            null,
            null,
            null,
            null,
            "North Gate, School");

    route.updateLocation(18.5204, 73.8567, 36.0, "North Gate");

    assertEquals(18.5204, route.getLatitude());
    assertEquals(73.8567, route.getLongitude());
    assertEquals(36.0, route.getSpeedKmvh());
    assertEquals("North Gate", route.getCurrentStop());
  }
}
