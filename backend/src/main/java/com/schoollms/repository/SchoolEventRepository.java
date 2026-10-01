package com.schoollms.repository;

import com.schoollms.domain.SchoolEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolEventRepository extends JpaRepository<SchoolEvent, Long> {
  List<SchoolEvent> findAllByOrderByEventDateAsc();
}
