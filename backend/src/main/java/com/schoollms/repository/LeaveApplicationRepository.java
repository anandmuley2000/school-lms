package com.schoollms.repository;

import com.schoollms.domain.LeaveApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
  List<LeaveApplication> findAllByOrderByCreatedAtDesc();

  List<LeaveApplication> findByRequester_EmailOrderByCreatedAtDesc(String email);
}
