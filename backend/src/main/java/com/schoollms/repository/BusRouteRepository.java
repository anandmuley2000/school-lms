package com.schoollms.repository;

import com.schoollms.domain.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusRouteRepository extends JpaRepository<BusRoute, Long> {
  List<BusRoute> findByStatus(String status);
}
