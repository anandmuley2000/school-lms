package com.schoollms.repository;

import com.schoollms.domain.BusRoute;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRouteRepository extends JpaRepository<BusRoute, Long> {
  List<BusRoute> findByStatus(String status);

  Optional<BusRoute> findByBusNumber(String busNumber);
}
