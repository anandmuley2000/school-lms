package com.schoollms.repository;

import com.schoollms.domain.FeeRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeRepository extends JpaRepository<FeeRecord, Long> {
  List<FeeRecord> findByStudent_IdOrderByDueDateDesc(Long studentId);

  boolean existsByStudent_IdAndDescription(Long studentId, String description);

  long countByStatus(com.schoollms.domain.FeeStatus status);

  void deleteByStudent_Id(Long studentId);
}
