package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
@Table(name = "bus_routes")
public class BusRoute {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String busNumber;

  @NotBlank
  @Column(nullable = false)
  private String routeName;

  @NotBlank
  @Column(nullable = false)
  private String driverName;

  @Column(nullable = false)
  private String driverPhone;

  private Integer capacity = 40;

  @Column(nullable = false)
  private String status = "IN_TRANSIT"; // ACTIVE, IN_TRANSIT, MAINTENANCE, INACTIVE

  private Double latitude = 18.5204; // default sample Pune / India coords

  private Double longitude = 73.8567;

  private Double speedKmvh = 35.0;

  private String currentStop = "Main Gate";

  @Column(length = 2000)
  private String stops = "North Gate, Central Junction, East Avenue, Main Campus";

  @Column(nullable = false)
  private Instant lastUpdated = Instant.now();

  protected BusRoute() {}

  public BusRoute(
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
      String stops) {
    updateDetails(busNumber, routeName, driverName, driverPhone, capacity, status, stops);
    updateLocation(latitude, longitude, speedKmvh, currentStop);
  }

  public void updateDetails(
      String busNumber,
      String routeName,
      String driverName,
      String driverPhone,
      Integer capacity,
      String status,
      String stops) {
    this.busNumber = busNumber;
    this.routeName = routeName;
    this.driverName = driverName;
    this.driverPhone = driverPhone;
    if (capacity != null) this.capacity = capacity;
    if (status != null) this.status = status;
    if (stops != null) this.stops = stops;
    this.lastUpdated = Instant.now();
  }

  public void updateLocation(Double latitude, Double longitude, Double speedKmvh, String currentStop) {
    if (latitude != null) this.latitude = latitude;
    if (longitude != null) this.longitude = longitude;
    if (speedKmvh != null) this.speedKmvh = speedKmvh;
    if (currentStop != null) this.currentStop = currentStop;
    this.lastUpdated = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getBusNumber() {
    return busNumber;
  }

  public String getRouteName() {
    return routeName;
  }

  public String getDriverName() {
    return driverName;
  }

  public String getDriverPhone() {
    return driverPhone;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public String getStatus() {
    return status;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public Double getSpeedKmvh() {
    return speedKmvh;
  }

  public String getCurrentStop() {
    return currentStop;
  }

  public String getStops() {
    return stops;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }
}
