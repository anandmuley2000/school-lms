package com.schoollms.repository;

import com.schoollms.domain.Attendance;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
  Optional<Attendance> findByStudent_IdAndDate(Long studentId, LocalDate date);

  List<Attendance> findByStudent_IdOrderByDateDesc(Long studentId);

  long countByDateAndStatus(LocalDate date, com.schoollms.domain.AttendanceStatus status);

  void deleteByStudent_Id(Long studentId);
}
